package com.xp.starsite.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.finance.dto.TransactionSaveDTO;
import com.xp.starsite.finance.entity.FinanceAccount;
import com.xp.starsite.finance.entity.FinanceTransaction;
import com.xp.starsite.finance.mapper.FinanceAccountMapper;
import com.xp.starsite.finance.mapper.FinanceTransactionMapper;
import com.xp.starsite.finance.service.TransactionService;
import com.xp.starsite.finance.vo.PageVO;
import com.xp.starsite.finance.vo.TransactionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 记账流水实现。不变式：流水与账户余额在同一数据库事务内变更，任何一笔流水
 * 都能由 balance_after 快照对账。删除回滚余额；转账整组回滚。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final int DIR_INCOME = 1;
    private static final int DIR_EXPENSE = 2;
    private static final int DIR_TRANSFER = 3;
    private static final int DIR_ADJUST = 4;

    private final FinanceAccountMapper accountMapper;
    private final FinanceTransactionMapper transactionMapper;

    @Override
    @Transactional
    public TransactionVO create(Long memberId, TransactionSaveDTO dto) {
        FinanceAccount account = requireOwnedAccount(memberId, dto.getAccountId());
        return switch (dto.getDirection()) {
            case DIR_INCOME -> doSingle(memberId, account, dto, DIR_INCOME, dto.getAmount());
            case DIR_EXPENSE -> doSingle(memberId, account, dto, DIR_EXPENSE, dto.getAmount());
            case DIR_ADJUST -> doAdjust(memberId, account, dto);
            case DIR_TRANSFER -> doTransfer(memberId, account, dto);
            default -> throw new BizException(ResultCode.BAD_REQUEST.getCode(), "不支持的记账类型");
        };
    }

    @Override
    public PageVO<TransactionVO> list(Long memberId, Long accountId, Integer direction,
                                      int year, int month, long page, long size) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        Page<FinanceTransaction> result = transactionMapper.selectPage(Page.of(page, size),
                new LambdaQueryWrapper<FinanceTransaction>()
                        .eq(FinanceTransaction::getOwnerMemberId, memberId)
                        .eq(accountId != null, FinanceTransaction::getAccountId, accountId)
                        .eq(direction != null, FinanceTransaction::getDirection, direction)
                        .ge(FinanceTransaction::getTransTime, start)
                        .lt(FinanceTransaction::getTransTime, end)
                        .orderByDesc(FinanceTransaction::getTransTime)
                        .orderByDesc(FinanceTransaction::getId));
        return PageVO.of(result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public TransactionVO getDetail(Long memberId, Long id) {
        return toVO(requireOwnedTransaction(memberId, id));
    }

    @Override
    @Transactional
    public void delete(Long memberId, Long id) {
        FinanceTransaction t = requireOwnedTransaction(memberId, id);
        if (t.getDirection() == DIR_TRANSFER) {
            // 转账整组回滚（防删一半导致两账户账实失衡）
            List<FinanceTransaction> group = transactionMapper.selectList(
                    new LambdaQueryWrapper<FinanceTransaction>()
                            .eq(FinanceTransaction::getTransferGroupId, t.getTransferGroupId()));
            for (FinanceTransaction leg : group) {
                rollbackOne(leg);
                transactionMapper.deleteById(leg.getId());
            }
            log.info("删除转账组: group={}, legs={}", t.getTransferGroupId(), group.size());
            return;
        }
        rollbackOne(t);
        transactionMapper.deleteById(id);
        log.info("删除流水: id={}, direction={}", id, t.getDirection());
    }

    /** 收入/支出：单账户，amount 恒正 */
    private TransactionVO doSingle(Long memberId, FinanceAccount account, TransactionSaveDTO dto,
                                   int direction, BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "金额必须大于 0");
        }
        BigDecimal delta = direction == DIR_INCOME ? amount : amount.negate();
        account.setBalance(account.getBalance().add(delta));
        accountMapper.updateById(account);

        FinanceTransaction t = baseTransaction(memberId, account, dto, direction, amount);
        t.setBalanceAfter(account.getBalance());
        transactionMapper.insert(t);
        return toVO(t);
    }

    /** 调整：差额校准，amount 可正可负，不计收支 */
    private TransactionVO doAdjust(Long memberId, FinanceAccount account, TransactionSaveDTO dto) {
        if (dto.getAmount().signum() == 0) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "调整差额不能为 0");
        }
        account.setBalance(account.getBalance().add(dto.getAmount()));
        accountMapper.updateById(account);

        FinanceTransaction t = baseTransaction(memberId, account, dto, DIR_ADJUST, dto.getAmount());
        t.setBalanceAfter(account.getBalance());
        t.setRemark(dto.getRemark() == null || dto.getRemark().isBlank()
                ? "余额校准" : dto.getRemark());
        transactionMapper.insert(t);
        return toVO(t);
    }

    /** 转账：转出-金额 + 转入+金额，两笔流水同组 */
    private TransactionVO doTransfer(Long memberId, FinanceAccount from, TransactionSaveDTO dto) {
        if (dto.getToAccountId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "转账须指定转入账户");
        }
        if (dto.getToAccountId().equals(from.getId())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "转入账户不能与转出账户相同");
        }
        if (dto.getAmount().signum() <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "金额必须大于 0");
        }
        FinanceAccount to = requireOwnedAccount(memberId, dto.getToAccountId());

        long groupId = com.baomidou.mybatisplus.core.toolkit.IdWorker.getId();
        from.setBalance(from.getBalance().subtract(dto.getAmount()));
        to.setBalance(to.getBalance().add(dto.getAmount()));
        accountMapper.updateById(from);
        accountMapper.updateById(to);

        FinanceTransaction out = baseTransaction(memberId, from, dto, DIR_TRANSFER, dto.getAmount().negate());
        out.setTransferGroupId(groupId);
        out.setBalanceAfter(from.getBalance());
        out.setCounterparty(to.getName());
        transactionMapper.insert(out);

        FinanceTransaction in = baseTransaction(memberId, to, dto, DIR_TRANSFER, dto.getAmount());
        in.setTransferGroupId(groupId);
        in.setBalanceAfter(to.getBalance());
        in.setCounterparty(from.getName());
        transactionMapper.insert(in);
        log.info("转账: {} -> {}, amount={}", from.getName(), to.getName(), dto.getAmount());
        return toVO(out);
    }

    /** 删除流水时的余额回滚（与记账互为逆操作；转账腿 amount 带符号，统一取反即回滚） */
    private void rollbackOne(FinanceTransaction t) {
        if (t.getBalanceAfter() == null) {
            return;
        }
        FinanceAccount account = accountMapper.selectById(t.getAccountId());
        if (account == null) {
            return;
        }
        BigDecimal delta = t.getDirection() == DIR_EXPENSE
                ? t.getAmount()
                : t.getAmount().negate();
        account.setBalance(account.getBalance().add(delta));
        accountMapper.updateById(account);
    }

    private FinanceTransaction baseTransaction(Long memberId, FinanceAccount account,
                                               TransactionSaveDTO dto, int direction, BigDecimal amount) {
        FinanceTransaction t = new FinanceTransaction();
        t.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
        t.setFamilyId(account.getFamilyId());
        t.setOwnerMemberId(memberId);
        t.setAccountId(account.getId());
        t.setAccountName(account.getName());
        t.setDirection(direction);
        t.setAmount(amount);
        t.setCategory(dto.getCategory());
        t.setCategoryName(com.xp.starsite.finance.FinanceCategories.labelOf(dto.getCategory()));
        t.setTransTime(dto.getTransTime());
        t.setCounterparty(dto.getCounterparty());
        t.setSource(1);
        t.setFileId(dto.getFileId());
        t.setRemark(dto.getRemark());
        return t;
    }

    private FinanceAccount requireOwnedAccount(Long memberId, Long id) {
        FinanceAccount account = accountMapper.selectById(id);
        if (account == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "账户不存在");
        }
        if (!account.getOwnerMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return account;
    }

    private FinanceTransaction requireOwnedTransaction(Long memberId, Long id) {
        FinanceTransaction t = transactionMapper.selectById(id);
        if (t == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "流水不存在");
        }
        if (!t.getOwnerMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return t;
    }

    private TransactionVO toVO(FinanceTransaction t) {
        TransactionVO vo = new TransactionVO();
        BeanUtils.copyProperties(t, vo);
        return vo;
    }
}

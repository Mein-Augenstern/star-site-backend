package com.xp.starsite.finance.service.impl;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.util.IdGenerator;
import com.xp.starsite.finance.FinanceCategories;
import com.xp.starsite.finance.dto.AccountSaveDTO;
import com.xp.starsite.finance.entity.FinanceAccount;
import com.xp.starsite.finance.entity.FinanceTransaction;
import com.xp.starsite.finance.repository.FinanceAccountRepository;
import com.xp.starsite.finance.repository.FinanceTransactionRepository;
import com.xp.starsite.finance.service.AccountService;
import com.xp.starsite.finance.vo.AccountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 资金账户实现。
 * 归属防线：财务资源直接挂 owner_member_id（无 familyMemberId 概念），按 id 取资源比对 owner，
 * 他人资源 403——与保单/档案同款 fail-closed。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final Set<String> ACCOUNT_TYPES = Set.of(
            "cash", "bank", "wechat", "alipay", "broker", "fund", "credit", "loan");

    private static final Set<String> LIABILITY_TYPES = Set.of("credit", "loan");

    private final FinanceAccountRepository accountRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final IdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountVO create(Long memberId, AccountSaveDTO dto) {
        validateType(dto.getAccountType());
        FinanceAccount account = new FinanceAccount();
        account.setId(idGenerator.nextId());
        account.setName(dto.getName());
        account.setAccountType(dto.getAccountType());
        account.setInitialBalance(dto.getInitialBalance() == null ? BigDecimal.ZERO : dto.getInitialBalance());
        account.setBalance(account.getInitialBalance());
        account.setLiabilityFlag(LIABILITY_TYPES.contains(dto.getAccountType()) ? 1 : 0);
        account.setInstitution(dto.getInstitution());
        account.setRemark(dto.getRemark());
        account.setOwnerMemberId(memberId);
        // family_id：V1 单人记账，建户时若已有家庭则冗余（多人协作后按 family 聚合）
        account.setFamilyId(resolveFamilyId(memberId));
        accountRepository.save(account);

        // 初始余额 > 0 记调整流水留痕（可溯）
        if (account.getInitialBalance().compareTo(BigDecimal.ZERO) != 0) {
            insertAdjustTransaction(account, account.getInitialBalance(), "建户初始余额");
        }
        log.info("建户: id={}, name={}, initial={}", account.getId(), account.getName(), account.getInitialBalance());
        return toVO(accountRepository.findById(account.getId()).orElse(null));
    }

    @Override
    public List<AccountVO> list(Long memberId) {
        return accountRepository.listByOwner(memberId).stream().map(this::toVO).toList();
    }

    @Override
    public AccountVO getDetail(Long memberId, Long id) {
        return toVO(requireOwnedAccount(memberId, id));
    }

    @Override
    public AccountVO update(Long memberId, Long id, AccountSaveDTO dto) {
        FinanceAccount account = requireOwnedAccount(memberId, id);
        validateType(dto.getAccountType());
        account.setName(dto.getName());
        account.setAccountType(dto.getAccountType());
        account.setInstitution(dto.getInstitution());
        account.setRemark(dto.getRemark());
        accountRepository.update(account);
        return toVO(account);
    }

    @Override
    public void delete(Long memberId, Long id) {
        FinanceAccount account = requireOwnedAccount(memberId, id);
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(),
                    "账户余额不为 0（当前 " + account.getBalance() + "），请先转账或调整至 0 再注销");
        }
        accountRepository.markDeleted(id);
        log.info("注销账户: id={}", id);
    }

    private FinanceAccount requireOwnedAccount(Long memberId, Long id) {
        FinanceAccount account = accountRepository.findById(id).orElse(null);
        if (account == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "账户不存在");
        }
        if (!account.getOwnerMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return account;
    }

    private void validateType(String type) {
        if (!ACCOUNT_TYPES.contains(type)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "不支持的账户类型");
        }
    }

    /** 供 TransactionService 回写余额与记调整流水（同域内共享，不走接口） */
    void insertAdjustTransaction(FinanceAccount account, BigDecimal delta, String reason) {
        FinanceTransaction t = new FinanceTransaction();
        t.setId(idGenerator.nextId());
        t.setFamilyId(account.getFamilyId());
        t.setOwnerMemberId(account.getOwnerMemberId());
        t.setAccountId(account.getId());
        t.setAccountName(account.getName());
        t.setDirection(4);
        t.setAmount(delta);
        t.setTransTime(java.time.LocalDateTime.now());
        t.setBalanceAfter(account.getBalance());
        t.setSource(1);
        t.setRemark(reason);
        transactionRepository.save(t);
    }

    Long resolveFamilyId(Long memberId) {
        // F1 简化：家庭 id 冗余留 0 占位，多人协作（阶段四）接 family 服务回填
        return 0L;
    }

    static String categoryLabel(String category) {
        return StringUtils.hasText(category) ? FinanceCategories.labelOf(category) : "";
    }

    private AccountVO toVO(FinanceAccount account) {
        AccountVO vo = new AccountVO();
        BeanUtils.copyProperties(account, vo);
        return vo;
    }
}

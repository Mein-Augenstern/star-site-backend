package com.xp.starsite.finance.service.impl;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.util.IdGenerator;
import com.xp.starsite.finance.entity.FinanceAccount;
import com.xp.starsite.finance.entity.FinanceTransaction;
import com.xp.starsite.finance.importer.BillParseResult;
import com.xp.starsite.finance.importer.BillParserRegistry;
import com.xp.starsite.finance.repository.FinanceAccountRepository;
import com.xp.starsite.finance.repository.FinanceImportLogRepository;
import com.xp.starsite.finance.repository.FinanceTransactionRepository;
import com.xp.starsite.finance.service.ImportService;
import com.xp.starsite.finance.service.ImportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 账单导入实现。
 * 幂等：uk_source_ext(source, external_id) 唯一键，重复导入的行 DuplicateKeyException 跳过；
 * 余额：解析批的净额（收入 + / 支出 -）一次增量更新账户，与流水同事务。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {

    private final BillParserRegistry parserRegistry;
    private final FinanceAccountRepository accountRepository;
    private final FinanceImportLogRepository importLogRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final IdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importBill(Long memberId, Long accountId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "请选择账单文件");
        }
        FinanceAccount account = requireOwnedAccount(memberId, accountId);
        return doImport(memberId, accountId, readText(file));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult doImport(Long memberId, Long accountId, String content) {
        FinanceAccount account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "账户不存在");
        }
        BillParseResult parsed = parserRegistry.detect(content).parse(content, accountId, java.util.Map.of());

        BigDecimal delta = BigDecimal.ZERO;
        int imported = 0;
        int duplicates = 0;
        for (FinanceTransaction t : parsed.getTransactions()) {
            t.setFamilyId(account.getFamilyId());
            t.setOwnerMemberId(memberId);
            t.setAccountName(account.getName());
            t.setBalanceAfter(null);
            t.setRemark(t.getRemark() == null ? "" : t.getRemark());
            t.setId(idGenerator.nextId());
            try {
                transactionRepository.save(t);
                imported++;
                // 收入加、支出减（金额恒正）
                delta = delta.add(t.getDirection() == 1 ? t.getAmount() : t.getAmount().negate());
            } catch (DuplicateKeyException e) {
                duplicates++;
            }
        }
        if (delta.signum() != 0) {
            // 原子增量（一锁二判三更新）：并发导入/记账下不丢更新
            accountRepository.updateBalanceDelta(account.getId(), delta);
            account.setBalance(account.getBalance().add(delta));
        }
        log.info("账单导入: channel={}, parsed={}, imported={}, dup={}, skipped={}, delta={}",
                parsed.getChannel(), parsed.getTransactions().size(), imported, duplicates,
                parsed.getSkipped(), delta);
        return new ImportResult(parsed.getChannel(), parsed.getTransactions().size(),
                imported, duplicates, parsed.getSkipped());
    }

    @Override
    public java.util.List<com.xp.starsite.finance.entity.FinanceImportLog> logs(Long memberId, Long fileId) {
        return importLogRepository.listByOwner(memberId, fileId);
    }

    private FinanceAccount requireOwnedAccount(Long memberId, Long accountId) {
        FinanceAccount account = accountRepository.findById(accountId).orElse(null);
        if (account == null || !account.getOwnerMemberId().equals(memberId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return account;
    }

    /** 编码探测：UTF-8 严格解码失败（含替换符）则按 GBK 重读——兼容微信真实导出 */
    private String readText(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String utf8 = new String(bytes, StandardCharsets.UTF_8);
            if (!utf8.contains("\uFFFD")) {
                // 去 BOM
                return utf8.startsWith("\uFEFF") ? utf8.substring(1) : utf8;
            }
            return new String(bytes, Charset.forName("GBK"));
        } catch (Exception e) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "账单文件读取失败");
        }
    }

    /** 编码探测（消费端复用）：UTF-8 严格解码失败按 GBK 重读 */
    public static String decode(byte[] bytes) {
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        if (!utf8.contains("\uFFFD")) {
            return utf8.startsWith("\uFEFF") ? utf8.substring(1) : utf8;
        }
        return new String(bytes, Charset.forName("GBK"));
    }
}

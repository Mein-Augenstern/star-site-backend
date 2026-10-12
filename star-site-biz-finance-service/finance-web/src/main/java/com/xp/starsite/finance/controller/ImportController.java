package com.xp.starsite.finance.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.finance.controller.actions.FinanceActions;
import com.xp.starsite.finance.service.ImportResult;
import com.xp.starsite.finance.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 账单导入接口（全部需 JWT）
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 finance.import.*。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/finance/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 导入微信/支付宝账单 CSV（multipart；账单流水挂指定账户）
     *
     * @param memberId  当前登录账号（JWT 注入）
     * @param accountId 目标账户 id
     * @param file      账单 CSV 文件
     * @return 导入结果（成功/跳过/失败计数）
     */
    @PostMapping
    public R<ImportResult> importBill(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                      @RequestParam("accountId") Long accountId,
                                      @RequestParam("file") MultipartFile file) {
        return writeTemplate.write(FinanceActions.IMPORT_START,
                () -> importService.importBill(memberId, accountId, file));
    }

    /**
     * 异步导入记录（file 直传 → FileUploaded 事件 → 本接口轮询结果）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param fileId   可选，按文件 id 过滤
     * @return 导入记录列表
     */
    @GetMapping("/logs")
    public R<java.util.List<com.xp.starsite.finance.entity.FinanceImportLog>> logs(
            @RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
            @RequestParam(value = "fileId", required = false) Long fileId) {
        return queryTemplate.query(FinanceActions.IMPORT_LOGS,
                () -> importService.logs(memberId, fileId));
    }
}

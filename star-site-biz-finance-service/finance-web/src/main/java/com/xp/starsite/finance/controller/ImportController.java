package com.xp.starsite.finance.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
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
 */
@RestController
@RequestMapping("/api/finance/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    /** 导入微信/支付宝账单 CSV（multipart；账单流水挂指定账户） */
    @PostMapping
    public R<ImportResult> importBill(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                      @RequestParam("accountId") Long accountId,
                                      @RequestParam("file") MultipartFile file) {
        return R.ok(importService.importBill(memberId, accountId, file));
    }

    /** 异步导入记录（file 直传 → FileUploaded 事件 → 本接口轮询结果） */
    @GetMapping("/logs")
    public R<java.util.List<com.xp.starsite.finance.entity.FinanceImportLog>> logs(
            @RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
            @RequestParam(value = "fileId", required = false) Long fileId) {
        return R.ok(importService.logs(memberId, fileId));
    }
}

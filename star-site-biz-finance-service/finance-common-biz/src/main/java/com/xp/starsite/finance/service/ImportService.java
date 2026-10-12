package com.xp.starsite.finance.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 账单导入服务（V1 同步直传；事件异步化见 技术/07 取舍说明）
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface ImportService {

    /**
     * 导入微信/支付宝账单 CSV。
     * 流程：编码探测（UTF-8 失败转 GBK，兼容微信真实导出）→ 格式探测选解析器 →
     * 逐行解析 → (source, external_id) 唯一键幂等入库 → 账户余额一次增量更新。
     *
     * @param memberId  当前登录账号
     * @param accountId 导入目标账户 id
     * @param file      账单 CSV 文件
     * @return 导入结果（渠道/解析数/新入账/重复/跳过）
     * @throws com.xp.starsite.common.exception.BizException 未选择文件（400）、
     *                                                       账户不存在或非本人账户（403）、文件读取失败（400）
     */
    ImportResult importBill(Long memberId, Long accountId, MultipartFile file);

    /**
     * 异步导入核心（FileUploaded 消费者调用）：账单文本 → 幂等入库 + 余额增量。
     * 与同步入口共享解析器与幂等键。
     *
     * @param memberId  当前登录账号
     * @param accountId 导入目标账户 id
     * @param content   账单文本内容
     * @return 导入结果（渠道/解析数/新入账/重复/跳过）
     * @throws com.xp.starsite.common.exception.BizException 账户不存在（400）
     */
    ImportResult doImport(Long memberId, Long accountId, String content);

    /**
     * 异步导入记录（按文件过滤，时间倒序）
     *
     * @param memberId 当前登录账号
     * @param fileId   可选，按上传文件 id 过滤
     * @return 导入日志列表
     */
    java.util.List<com.xp.starsite.finance.entity.FinanceImportLog> logs(Long memberId, Long fileId);
}

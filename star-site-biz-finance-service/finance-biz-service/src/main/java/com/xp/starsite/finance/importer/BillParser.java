package com.xp.starsite.finance.importer;

import java.util.Map;

/**
 * 账单解析器（策略模式）：每渠道一个实现，按渠道名从 {@link BillParserRegistry} 取。
 * 实现只负责"文本行 → 流水草稿"，落库/去重/余额由导入服务统一处理。
 */
public interface BillParser {

    /** 渠道名（wechat/alipay） */
    String channel();

    /**
     * 是否能解析该账单文本（格式探测：按头部特征行判断）
     */
    boolean supports(String content);

    /**
     * 解析账单文本。
     *
     * @param content  账单全文（已按 UTF-8 读出；真实微信导出为 GBK，前端上传后由后端统一转码探测）
     * @param accountId 导入目标账户（流水挂此账户）
     * @param context  解析上下文（transTime 等公共填充）
     */
    BillParseResult parse(String content, Long accountId, Map<String, Object> context);
}

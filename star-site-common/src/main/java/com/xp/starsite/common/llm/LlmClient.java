package com.xp.starsite.common.llm;

/**
 * LLM 客户端（业务域面向此接口编程；GLM/OpenAI 兼容协议实现见 StarSiteLlmConfig）。
 * 未配置 llm.api-key 时容器中不存在本 Bean，调用方须容忍（ObjectProvider）。
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface LlmClient {

    /**
     * 单轮对话：system 设角色约束，user 给数据。
     *
     * @return 模型文本回复
     * @throws LlmException 调用失败（网络/鉴权/限流）
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 单轮多模态：imageDataUrl 为 data:image/xxx;base64,... 形式的图片。
     *
     * @return 模型文本回复
     * @throws LlmException 调用失败（网络/鉴权/限流）
     */
    String chatWithImage(String systemPrompt, String userPrompt, String imageDataUrl);

    /** LLM 调用异常（业务方翻译成友好提示） */
    class LlmException extends RuntimeException {

        public LlmException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

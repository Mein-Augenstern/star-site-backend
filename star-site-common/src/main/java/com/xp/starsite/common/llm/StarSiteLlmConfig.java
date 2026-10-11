package com.xp.starsite.common.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * LLM 显式装配（与 MQ 同款模式）：配置 llm.api-key 后才有 LlmClient Bean——
 * 未配置时调用方用 ObjectProvider 容忍缺失，输出"AI 解读未启用"降级提示。
 * 默认智谱 GLM（OpenAI 兼容协议），换任何兼容服务只改 base-url/model。
 * <p>
 * 全部参数配置化（代码零写死）：llm.base-url / llm.api-key / llm.model /
 * llm.vision-model / llm.temperature / llm.vision-temperature /
 * llm.connect-timeout-ms / llm.read-timeout-ms。
 */
@Slf4j
@Configuration
@ConditionalOnProperty("llm.api-key")
public class StarSiteLlmConfig {

    @Bean
    public LlmClient llmClient(
            @Value("${llm.base-url:https://open.bigmodel.cn/api/paas/v4}") String baseUrl,
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.model:glm-4-flash}") String model,
            @Value("${llm.vision-model:glm-4v-flash}") String visionModel,
            @Value("${llm.temperature:0.6}") double temperature,
            @Value("${llm.vision-temperature:0.1}") double visionTemperature,
            @Value("${llm.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${llm.read-timeout-ms:60000}") int readTimeoutMs) {
        // 连接/读取超时配置化：LLM 外呼不能依赖 JVM 默认（可能无限挂起占满线程）
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .requestFactory(factory)
                .build();
        log.info("LlmClient 就绪: base={}, model={}, timeouts={}/{}ms",
                baseUrl, model, connectTimeoutMs, readTimeoutMs);
        return new LlmClient() {

            private final ObjectMapper mapper = new ObjectMapper();

            @Override
            public String chat(String systemPrompt, String userPrompt) {
                String body = """
                        {
                          "model": "%s",
                          "messages": [
                            {"role": "system", "content": "%s"},
                            {"role": "user", "content": "%s"}
                          ],
                          "temperature": %s
                        }
                        """.formatted(model, escape(systemPrompt), escape(userPrompt), temperature);
                try {
                    JsonNode resp = client.post()
                            .uri("/chat/completions")
                            .header("Content-Type", "application/json")
                            .body(body)
                            .retrieve()
                            .body(JsonNode.class);
                    JsonNode content = resp == null ? null
                            : resp.path("choices").path(0).path("message").path("content");
                    if (content.isMissingNode() || content.asText().isBlank()) {
                        throw new LlmException("LLM 返回为空", null);
                    }
                    return content.asText();
                } catch (LlmException e) {
                    throw e;
                } catch (Exception e) {
                    log.warn("LLM 调用失败: {}", e.getMessage());
                    throw new LlmException("LLM 调用失败: " + e.getMessage(), e);
                }
            }

            @Override
            public String chatWithImage(String systemPrompt, String userPrompt, String imageDataUrl) {
                String body;
                try {
                    body = mapper.writeValueAsString(java.util.Map.of(
                            "model", visionModel,
                            "messages", java.util.List.of(
                                    java.util.Map.of("role", "system", "content", systemPrompt),
                                    java.util.Map.of("role", "user", "content", java.util.List.of(
                                            java.util.Map.of("type", "text", "text", userPrompt),
                                    java.util.Map.of("type", "image_url",
                                            "image_url", java.util.Map.of("url", imageDataUrl))))),
                            "temperature", visionTemperature));
                } catch (Exception e) {
                    throw new LlmException("多模态请求构造失败", e);
                }
                try {
                    JsonNode resp = client.post()
                            .uri("/chat/completions")
                            .header("Content-Type", "application/json")
                            .body(body)
                            .retrieve()
                            .body(JsonNode.class);
                    JsonNode content = resp == null ? null
                            : resp.path("choices").path(0).path("message").path("content");
                    if (content.isMissingNode() || content.asText().isBlank()) {
                        throw new LlmException("LLM 返回为空", null);
                    }
                    return content.asText();
                } catch (LlmException e) {
                    throw e;
                } catch (Exception e) {
                    log.warn("LLM 多模态调用失败: {}", e.getMessage());
                    throw new LlmException("LLM 调用失败: " + e.getMessage(), e);
                }
            }

            /** JSON 字符串字面量转义（引号/反斜杠/换行），避免手拼 payload 破坏 JSON */
            private String escape(String s) {
                try {
                    return mapper.writeValueAsString(s).replaceAll("^\"|\"$", "");
                } catch (Exception e) {
                    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                }
            }
        };
    }
}

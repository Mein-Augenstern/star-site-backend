package com.xp.starsite.health.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xp.starsite.common.async.AsyncCommand;
import com.xp.starsite.common.async.AsyncCommandExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 解读异步命令执行器（star-async 首个业务用例）：
 * 解读调用 LLM 耗时 30s+，异步化后提交方事务内落命令即返回，本执行器在后台完成
 * LLM 调用并落库缓存；失败由组件按指数退避重试，超限 DEAD。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiInterpretExecutor implements AsyncCommandExecutor {

    public static final String TYPE = "AI_INTERPRET";

    private final ReportInterpretationService interpretationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void execute(AsyncCommand cmd) throws Exception {
        JsonNode payload = objectMapper.readTree(cmd.getPayload());
        long memberId = payload.path("memberId").asLong();
        long reportId = payload.path("reportId").asLong();
        boolean force = payload.path("force").asBoolean(false);
        interpretationService.interpret(memberId, reportId, force);
    }
}

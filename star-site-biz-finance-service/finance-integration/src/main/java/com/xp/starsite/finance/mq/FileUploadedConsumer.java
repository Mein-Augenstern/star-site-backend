package com.xp.starsite.finance.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xp.starsite.api.client.FileApi;
import com.xp.starsite.api.event.file.FileUploadedEvent;
import com.xp.starsite.common.mq.AbstractIdempotentConsumer;
import com.xp.starsite.common.rpc.RpcCallGuard;
import com.xp.starsite.common.mq.EnvelopeDecoder;
import com.xp.starsite.common.mq.event.EventEnvelope;
import com.xp.starsite.common.mq.event.EventTypes;
import com.xp.starsite.finance.entity.FinanceImportLog;
import com.xp.starsite.finance.repository.FinanceImportLogRepository;
import com.xp.starsite.finance.service.ImportService;
import com.xp.starsite.finance.service.ImportResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

/**
 * 文件上传事件消费者（TOPIC_FILE 首个实战）：bizType=bill_import 的上传
 * 触发异步账单导入——FileApi 拉取内容 → 复用解析器/幂等入库 → 结果落 import_log。
 * <p>
 * 失败语义：解码失败抛出走 DLQ；业务失败（格式无法识别/账户不存在等）记失败日志不重试
 * （重试同样会失败），前端经 /api/finance/import/logs 轮询可见。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@RocketMQMessageListener(topic = "TOPIC_FILE", consumerGroup = "G_finance_file")
public class FileUploadedConsumer extends AbstractIdempotentConsumer implements RocketMQListener<String> {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String BIZ_BILL_IMPORT = "bill_import";

    private final FileApi fileApi;
    private final RpcCallGuard rpc;
    private final ImportService importService;
    private final FinanceImportLogRepository importLogRepository;

    public FileUploadedConsumer(JdbcTemplate jdbcTemplate, FileApi fileApi, RpcCallGuard rpc,
                                ImportService importService, FinanceImportLogRepository importLogRepository) {
        super(jdbcTemplate, "G_finance_file");
        this.fileApi = fileApi;
        this.rpc = rpc;
        this.importService = importService;
        this.importLogRepository = importLogRepository;
    }

    @Override
    public void onMessage(String raw) {
        EventEnvelope env = EnvelopeDecoder.decode(raw);
        if (!EventTypes.FILE_UPLOADED.equals(env.getEventType())) {
            return;
        }
        consumeIdempotent("TOPIC_FILE", env.getEventType(), env.getEventId(),
                env.getTraceId(), () -> dispatch(env));
    }

    private void dispatch(EventEnvelope env) {
        FileUploadedEvent event;
        try {
            event = MAPPER.readValue(env.getPayloadJson(), FileUploadedEvent.class);
        } catch (Exception e) {
            throw new IllegalStateException("FileUploaded 事件反序列化失败", e);
        }
        if (!BIZ_BILL_IMPORT.equals(event.getBizType())) {
            log.debug("非账单文件，跳过: bizType={}", event.getBizType());
            return;
        }
        Map<String, String> attrs = event.getAttributes();
        Long accountId = attrs == null ? null : parseLong(attrs.get(FileUploadedEvent.ATTR_ACCOUNT_ID));
        Long memberId = event.getUploaderMemberId();
        if (accountId == null || memberId == null) {
            log.warn("账单导入事件缺少 accountId/memberId，跳过: fileId={}", event.getFileId());
            return;
        }
        handleBillImport(memberId, accountId, event.getFileId());
    }

    private void handleBillImport(Long memberId, Long accountId, Long fileId) {
        FinanceImportLog logRow = new FinanceImportLog();
        logRow.setOwnerMemberId(memberId);
        logRow.setAccountId(accountId);
        logRow.setFileId(fileId);
        try {
            // RPC 经统一门卫（fail-closed）：失败抛 RpcException 落下方 catch → import_log status=2（消息不重试）
            byte[] content = rpc.call("finance.file.content", () -> fileApi.content(fileId));
            if (content == null) {
                throw new IllegalStateException("账单文件内容为空");
            }
            String text = com.xp.starsite.finance.service.impl.ImportServiceImpl.decode(content);
            ImportResult result = importService.doImport(memberId, accountId, text);
            logRow.setChannel(result.getChannel());
            logRow.setParsed(result.getParsed());
            logRow.setImported(result.getImported());
            logRow.setDuplicates(result.getDuplicates());
            logRow.setSkipped(result.getSkipped());
            logRow.setStatus(1);
            log.info("异步账单导入完成: file={}, imported={}", fileId, result.getImported());
        } catch (Exception e) {
            logRow.setChannel("unknown");
            logRow.setStatus(2);
            logRow.setError(truncate(e.getMessage(), 255));
            log.error("异步账单导入失败: file={}, err={}", fileId, e.getMessage());
        }
        importLogRepository.save(logRow);
    }

    private static Long parseLong(String s) {
        try {
            return s == null ? null : Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}

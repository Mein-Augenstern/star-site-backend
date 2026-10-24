package com.xp.starsite.health.integration;

import com.xp.starsite.api.client.FileApi;
import com.xp.starsite.common.rpc.RpcIntegration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 文件服务集成层（SOFA integration 层）：health 域读文件内容的唯一出口。
 * 领域内其他类禁止直接依赖 file 的 Feign（ReportOcrService 取图走本层）。
 * 文件读取是 OCR 识别的强依赖，fail-closed：失败经 RpcCallGuard 翻译为
 * 标准 RpcException（带 retryable）上抛。
 *
 * @author muyi
 * @since 2026-10
 */
@Component
@RpcIntegration("star-site-platform-file")
@RequiredArgsConstructor
public class FileIntegration {

    private final FileApi fileApi;

    /**
     * 读文件内容（字节数组）
     *
     * @throws com.xp.starsite.common.rpc.RpcException 文件服务不可用（fail-closed，retryable=true）
     *         或文件不存在（透传 400 语义，retryable=false）
     */
    public byte[] content(Long fileId) {
        return fileApi.content(fileId).getData();
    }
}

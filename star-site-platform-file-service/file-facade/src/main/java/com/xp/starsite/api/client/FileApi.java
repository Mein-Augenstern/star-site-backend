package com.xp.starsite.api.client;

import com.xp.starsite.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * file 服务内部接口（Feign 契约）：服务间拉取文件内容（如账单导入解析）。
 * /internal/** 不经网关路由，仅限内网。
 */
@FeignClient(name = "star-site-platform-file", contextId = "fileInternalApi", path = "/internal/file")
public interface FileApi {

    /** 文件内容（≤20MB；账单 CSV 量级远小于此） */
    @GetMapping("/{id}/content")
    R<byte[]> content(@PathVariable("id") Long fileId);
}

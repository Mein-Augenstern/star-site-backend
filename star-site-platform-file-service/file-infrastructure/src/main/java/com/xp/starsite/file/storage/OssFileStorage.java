package com.xp.starsite.file.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * 阿里云 OSS 存储（file.storage-type=oss 时启用）。
 * bucket 保持私有，文件一律经本服务流式转发（JWT 保护）；
 * ECS 上把 OSS_ENDPOINT 换成同地域内网 endpoint 可免流量费。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "file.storage-type", havingValue = "oss")
public class OssFileStorage implements FileStorage {

    private final OSS ossClient;
    private final String bucket;

    public OssFileStorage(@Value("${file.oss.endpoint}") String endpoint,
                          @Value("${file.oss.access-key-id}") String accessKeyId,
                          @Value("${file.oss.access-key-secret}") String accessKeySecret,
                          @Value("${file.oss.bucket}") String bucket) {
        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(accessKeyId)
                || !StringUtils.hasText(accessKeySecret) || !StringUtils.hasText(bucket)) {
            throw new IllegalStateException(
                    "OSS 配置缺失：请设置 OSS_ENDPOINT / OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET / OSS_BUCKET");
        }
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        this.bucket = bucket;
        log.info("文件存储已切换为阿里云 OSS: endpoint={}, bucket={}", endpoint, bucket);
    }

    @Override
    public String save(InputStream in, String originalFilename, String contentType) {
        String key = StorageKeys.newKey(originalFilename);
        ObjectMetadata metadata = new ObjectMetadata();
        if (StringUtils.hasText(contentType)) {
            metadata.setContentType(contentType);
        }
        try {
            ossClient.putObject(bucket, key, in, metadata);
        } catch (Exception e) {
            log.error("OSS 上传失败: bucket={}, key={}", bucket, key, e);
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "文件上传失败");
        }
        return key;
    }

    @Override
    public InputStream open(String storageKey) {
        try {
            OSSObject object = ossClient.getObject(bucket, storageKey);
            return object.getObjectContent();
        } catch (Exception e) {
            log.error("OSS 读取失败: bucket={}, key={}", bucket, storageKey, e);
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "文件不存在");
        }
    }

    @PreDestroy
    public void shutdown() {
        ossClient.shutdown();
    }
}

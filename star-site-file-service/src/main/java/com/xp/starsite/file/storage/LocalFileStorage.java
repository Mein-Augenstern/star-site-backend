package com.xp.starsite.file.storage;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘存储（默认；file.storage-type=local 或未配置时启用）
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "file.storage-type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final Path rootDir;

    public LocalFileStorage(@Value("${file.storage-dir:./data/files}") String storageDir) {
        this.rootDir = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建文件存储目录: " + rootDir, e);
        }
        log.info("本地文件存储目录: {}", rootDir);
    }

    @Override
    public String save(InputStream in, String originalFilename, String contentType) {
        String key = StorageKeys.newKey(originalFilename);
        Path target = rootDir.resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("文件保存失败: {}", target, e);
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "文件保存失败");
        }
        return key;
    }

    @Override
    public InputStream open(String storageKey) {
        Path target = rootDir.resolve(storageKey).normalize();
        if (!target.startsWith(rootDir) || !Files.exists(target)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "文件不存在");
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "文件读取失败");
        }
    }
}

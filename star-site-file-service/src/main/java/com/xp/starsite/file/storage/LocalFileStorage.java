package com.xp.starsite.file.storage;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地磁盘存储：{rootDir}/{yyyyMM}/{uuid}.{ext}
 */
@Slf4j
@Component
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
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String ext = extOf(originalFilename);
        String key = month + "/" + UUID.randomUUID() + ext;
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

    private String extOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot).toLowerCase();
    }
}

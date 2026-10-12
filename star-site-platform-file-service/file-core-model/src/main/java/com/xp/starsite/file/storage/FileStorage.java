package com.xp.starsite.file.storage;

import java.io.InputStream;

/**
 * 文件存储抽象：本地磁盘 / OSS 可切换
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface FileStorage {

    /** 保存并返回存储键（如 202610/uuid.pdf） */
    String save(InputStream in, String originalFilename, String contentType);

    /** 打开读取流 */
    InputStream open(String storageKey);
}

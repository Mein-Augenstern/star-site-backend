package com.xp.starsite.file.storage;

import java.io.InputStream;

/**
 * 文件存储抽象：本地磁盘 / OSS 可切换
 *
 * @author muyi
 * @since 2026-10
 */
public interface FileStorage {

    /**
     * 保存文件内容
     *
     * @param in              文件输入流（由调用方关闭）
     * @param originalFilename 原始文件名（用于推导扩展名）
     * @param contentType      内容类型
     * @return 存储键（如 202610/uuid.pdf，读取时回传给 {@link #open}）
     */
    String save(InputStream in, String originalFilename, String contentType);

    /**
     * 打开读取流
     *
     * @param storageKey 存储键（save 返回值）
     * @return 文件内容输入流（由调用方关闭）
     */
    InputStream open(String storageKey);
}

package com.xp.starsite.file.storage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 存储键生成：{yyyyMM}/{uuid}{.ext}，本地与 OSS 共用
 *
 * @author muyi
 * @since 2026-10
 */
public final class StorageKeys {

    private StorageKeys() {
    }

    public static String newKey(String originalFilename) {
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return month + "/" + UUID.randomUUID() + extOf(originalFilename);
    }

    private static String extOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot).toLowerCase();
    }
}

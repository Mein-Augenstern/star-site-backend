package com.xp.starsite.file.service;

import com.xp.starsite.file.entity.FileRecord;
import com.xp.starsite.file.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface FileService {

    /**
     * 上传：校验类型/大小 → 落盘 → 建记录 → 发 FileUploaded 事件（TOPIC_FILE）。
     *
     * @param metadata 业务元数据（随事件透传给消费方，如 bill_import 的 accountId；可空）
     */
    FileVO upload(MultipartFile file, String bizType, Long uploaderMemberId,
                  java.util.Map<String, String> metadata);

    /** 按 id 取记录（不存在抛业务异常） */
    FileRecord requireFile(Long id);
}

package com.xp.starsite.file.service;

import com.xp.starsite.file.entity.FileRecord;
import com.xp.starsite.file.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务
 */
public interface FileService {

    /** 上传：校验类型/大小 → 落盘 → 建记录 */
    FileVO upload(MultipartFile file, String bizType, Long uploaderMemberId);

    /** 按 id 取记录（不存在抛业务异常） */
    FileRecord requireFile(Long id);
}

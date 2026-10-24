package com.xp.starsite.file.service;

import com.xp.starsite.file.entity.FileRecord;
import com.xp.starsite.file.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务
 *
 * @author muyi
 * @since 2026-10
 */
public interface FileService {

    /**
     * 上传：校验类型/大小 → 落盘 → 建记录 → 发 FileUploaded 事件（TOPIC_FILE）。
     *
     * @param file              上传的文件
     * @param bizType           业务类型（图片/账单等，用于类型白名单校验）
     * @param uploaderMemberId  上传者账号 id
     * @param metadata          业务元数据（随事件透传给消费方，如 bill_import 的 accountId；可空）
     * @return 文件视图（id/URL 等）
     * @throws com.xp.starsite.common.exception.BizException 未选择文件（400）、超过 20MB（400）、
     *                                                       不支持的文件类型（400）、落盘失败（500）
     */
    FileVO upload(MultipartFile file, String bizType, Long uploaderMemberId,
                  java.util.Map<String, String> metadata);

    /**
     * 按 id 取记录（不存在抛业务异常）
     *
     * @param id 文件记录 id
     * @return 文件记录
     * @throws com.xp.starsite.common.exception.BizException 文件不存在（400）
     */
    FileRecord requireFile(Long id);

    /**
     * 按 id 取本人文件（越权防线：仅上传者本人可读，多人协作后升级为家庭范围校验）
     *
     * @param memberId 当前登录账号（与记录上传者比对）
     * @param id       文件记录 id
     * @return 文件记录
     * @throws com.xp.starsite.common.exception.BizException 文件不存在（400）、非上传者本人（403）
     */
    FileRecord requireOwnedFile(Long memberId, Long id);

    /**
     * 解析文件内容类型为规范化 MIME 字符串：空/非法回落 application/octet-stream（保底下载语义）。
     * 预览响应头直接使用该字符串（HTTP 表达仍由 Controller 组装，媒体类型解析属文件域业务规则）。
     *
     * @param contentType 记录存储的 MIME 类型（可空）
     * @return 规范化 MIME 字符串（非法/缺省为 application/octet-stream）
     */
    String resolveContentType(String contentType);
}

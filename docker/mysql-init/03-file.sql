-- 文件域（star-site-file-service）
USE star_site;

CREATE TABLE IF NOT EXISTS `file` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `file_name`           VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `storage_key`         VARCHAR(255) NOT NULL COMMENT '存储键(相对路径/OSS key)',
  `content_type`        VARCHAR(128)          DEFAULT NULL,
  `size_bytes`          BIGINT        NOT NULL DEFAULT 0,
  `biz_type`            VARCHAR(16)   NOT NULL DEFAULT 'other' COMMENT 'avatar/report/other',
  `uploader_member_id`  BIGINT UNSIGNED       DEFAULT NULL COMMENT '上传者账号id',
  `created_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_uploader` (`uploader_member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件表';

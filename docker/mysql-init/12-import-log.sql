-- 账单导入流水（事件异步化：FileUploaded 消费结果记录，前端轮询）
USE star_site;

CREATE TABLE IF NOT EXISTS `finance_import_log` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `owner_member_id` BIGINT UNSIGNED NOT NULL COMMENT '发起账号id',
  `account_id`      BIGINT UNSIGNED NOT NULL COMMENT '导入目标账户',
  `file_id`         BIGINT UNSIGNED NOT NULL COMMENT '账单文件id(file服务)',
  `channel`         VARCHAR(16)              DEFAULT '' COMMENT 'wechat/alipay/unknown',
  `parsed`          INT             NOT NULL DEFAULT 0 COMMENT '解析笔数',
  `imported`        INT             NOT NULL DEFAULT 0 COMMENT '新入账笔数',
  `duplicates`      INT             NOT NULL DEFAULT 0 COMMENT '重复跳过',
  `skipped`         INT             NOT NULL DEFAULT 0 COMMENT '解析跳过',
  `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '1成功 2失败',
  `error`           VARCHAR(255)             DEFAULT '' COMMENT '失败原因',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner_file` (`owner_member_id`, `file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账单导入记录';

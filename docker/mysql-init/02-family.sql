-- 家庭档案域（star-site-family-service）
USE star_site;

CREATE TABLE IF NOT EXISTS `family` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`            VARCHAR(64)  NOT NULL COMMENT '家庭名称',
  `owner_member_id` BIGINT UNSIGNED NOT NULL COMMENT '创建者账号id(member服务)',
  `remark`          VARCHAR(255)          DEFAULT '' COMMENT '备注',
  `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner_member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭表';

CREATE TABLE IF NOT EXISTS `family_member` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `family_id`         BIGINT UNSIGNED NOT NULL COMMENT '家庭id',
  `name`              VARCHAR(32)  NOT NULL COMMENT '姓名',
  `relation`          VARCHAR(16)  NOT NULL COMMENT 'self/spouse/father/mother/child/other',
  `gender`            TINYINT      NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
  `birth_date`        DATE                  DEFAULT NULL COMMENT '出生日期',
  `phone`             VARCHAR(20)           DEFAULT NULL,
  `avatar_file_id`    BIGINT UNSIGNED       DEFAULT NULL COMMENT '头像文件id(file服务)',
  `linked_member_id`  BIGINT UNSIGNED       DEFAULT NULL COMMENT '绑定登录账号id(可空)',
  `remark`            VARCHAR(255)          DEFAULT '',
  `deleted`           TINYINT      NOT NULL DEFAULT 0,
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_family` (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭成员(自然人档案)';

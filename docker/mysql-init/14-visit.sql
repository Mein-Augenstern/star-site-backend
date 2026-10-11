-- 就医记录域（star-site-health-service，PRD P0 #5）
USE star_site;

CREATE TABLE IF NOT EXISTS `visit_record` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `family_member_id`  BIGINT UNSIGNED NOT NULL COMMENT '家庭成员id(family服务自然人档案)',
  `visit_date`        DATE     NOT NULL COMMENT '就诊日期',
  `hospital`          VARCHAR(128) NOT NULL COMMENT '医院',
  `department`        VARCHAR(64)  DEFAULT '' COMMENT '科室',
  `diagnosis`         VARCHAR(255) DEFAULT '' COMMENT '诊断',
  `medication`        VARCHAR(500) DEFAULT '' COMMENT '用药/处方摘要',
  `cost`              DECIMAL(12,2) DEFAULT NULL COMMENT '费用(元)',
  `remark`            VARCHAR(255) DEFAULT '',
  `deleted`           TINYINT      NOT NULL DEFAULT 0,
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_member_date` (`family_member_id`, `visit_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='就医记录';

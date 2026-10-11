-- 日常健康指标（慢病自测/孕期监测，PRD P1：孕妇+老人高频测量）
USE star_site;

CREATE TABLE IF NOT EXISTS `daily_metric` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `family_member_id`  BIGINT UNSIGNED NOT NULL COMMENT '家庭成员id',
  `metric_date`       DATE     NOT NULL COMMENT '测量日期',
  `metric_type`       VARCHAR(16) NOT NULL COMMENT '类型: weight/bp_sys/bp_dia/glucose/heart_rate',
  `value_num`         DECIMAL(10,2) NOT NULL COMMENT '数值',
  `remark`            VARCHAR(255) DEFAULT '',
  `deleted`           TINYINT      NOT NULL DEFAULT 0,
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_member_type_date` (`family_member_id`, `metric_type`, `metric_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日常健康指标';

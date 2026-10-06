-- 体检报告域（star-site-health-service）
USE star_site;

CREATE TABLE IF NOT EXISTS `health_report` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `family_member_id`  BIGINT UNSIGNED NOT NULL COMMENT '家庭成员id(family服务自然人档案)',
  `report_year`       SMALLINT UNSIGNED NOT NULL COMMENT '报告年份',
  `checkup_date`      DATE                  DEFAULT NULL COMMENT '体检日期',
  `hospital`          VARCHAR(128)          DEFAULT '' COMMENT '医院',
  `title`             VARCHAR(128)          DEFAULT '' COMMENT '标题',
  `summary`           VARCHAR(1000)         DEFAULT '' COMMENT '总结',
  `file_id`           BIGINT UNSIGNED       DEFAULT NULL COMMENT '报告文件id(file服务)',
  `remark`            VARCHAR(255)          DEFAULT '',
  `deleted`           TINYINT      NOT NULL DEFAULT 0,
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_member_year` (`family_member_id`, `report_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='体检报告';

CREATE TABLE IF NOT EXISTS `health_indicator` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `report_id`         BIGINT UNSIGNED NOT NULL COMMENT '报告id',
  `family_member_id`  BIGINT UNSIGNED NOT NULL COMMENT '家庭成员id(冗余)',
  `report_year`       SMALLINT UNSIGNED NOT NULL COMMENT '报告年份(冗余,趋势查询免join)',
  `item_name`         VARCHAR(64)  NOT NULL COMMENT '指标名,如 空腹血糖',
  `item_group`        VARCHAR(32)           DEFAULT '' COMMENT '分组,如 血常规',
  `value_num`         DECIMAL(12,4)         DEFAULT NULL COMMENT '数值结果',
  `value_text`        VARCHAR(64)           DEFAULT NULL COMMENT '文本结果(阴/阳等)',
  `unit`              VARCHAR(32)           DEFAULT '' COMMENT '单位',
  `ref_low`           DECIMAL(12,4)         DEFAULT NULL COMMENT '参考下限',
  `ref_high`          DECIMAL(12,4)         DEFAULT NULL COMMENT '参考上限',
  `abnormal_flag`     TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常 1偏高 2偏低',
  `sort_no`           INT          NOT NULL DEFAULT 0,
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report` (`report_id`),
  KEY `idx_member_item` (`family_member_id`, `item_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='体检指标';

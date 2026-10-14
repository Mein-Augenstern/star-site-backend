-- 复查闭环域（star-site-biz-recheck-service）
-- 注意：MySQL 容器 volume 已初始化过的话，本文件不会自动执行，需手动跑一次
USE star_site;

-- 复查计划：垂直产品「复查闭环」主体表。
-- 报告/指标数据在 health（横向服务），本表只存基线快照与闭环状态；
-- 提醒/每日监测规则在 reminder（横向服务），经 (source, biz_type=recheck_review|recheck_daily, biz_id=计划id) 幂等关联。
CREATE TABLE IF NOT EXISTS `recheck_plan` (
  `id`                    BIGINT UNSIGNED NOT NULL COMMENT '主键(雪花id,应用侧赋值)',
  `family_id`             BIGINT UNSIGNED NOT NULL COMMENT '家庭id',
  `owner_member_id`       BIGINT UNSIGNED NOT NULL COMMENT '归属顶梁柱账号id',
  `family_member_id`      BIGINT UNSIGNED NOT NULL COMMENT '目标成员档案id',
  `source_report_id`      BIGINT UNSIGNED NOT NULL COMMENT '基线报告id(health)',
  `source_report_title`   VARCHAR(128)         DEFAULT NULL COMMENT '基线报告标题快照',
  `source_indicator_id`   BIGINT UNSIGNED NOT NULL COMMENT '基线指标id(health)',
  `item_name`             VARCHAR(64)  NOT NULL COMMENT '指标名(闭环按名对齐新报告)',
  `baseline_value_num`    DECIMAL(12,3)        DEFAULT NULL COMMENT '基线数值',
  `baseline_value_text`   VARCHAR(64)          DEFAULT NULL COMMENT '基线结果文本(定性指标)',
  `unit`                  VARCHAR(32)          DEFAULT NULL COMMENT '单位快照',
  `ref_low`               DECIMAL(12,3)        DEFAULT NULL COMMENT '参考区间下限快照',
  `ref_high`              DECIMAL(12,3)        DEFAULT NULL COMMENT '参考区间上限快照',
  `baseline_abnormal_flag` TINYINT             DEFAULT NULL COMMENT '基线异常:0正常 1偏高 2偏低',
  `target_date`           DATE         NOT NULL COMMENT '目标复查日期(默认+90天)',
  `metric_type`           VARCHAR(32)          DEFAULT NULL COMMENT '关联日常监测类型(bp_sys/weight...,空=不挂每日监测)',
  `status`                TINYINT      NOT NULL DEFAULT 1 COMMENT '1进行中 2已闭环 3已取消',
  `closed_report_id`      BIGINT UNSIGNED      DEFAULT NULL COMMENT '复查新报告id(闭环回填)',
  `closed_report_title`   VARCHAR(128)         DEFAULT NULL COMMENT '复查新报告标题快照',
  `conclusion`            VARCHAR(255)         DEFAULT NULL COMMENT '闭环对比结论',
  `remark`                VARCHAR(255)         DEFAULT NULL COMMENT '备注',
  `deleted`               TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常 1已删',
  `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner_member_id`, `status`),
  KEY `idx_member` (`family_member_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='复查计划';

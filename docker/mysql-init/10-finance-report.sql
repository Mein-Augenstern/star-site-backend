-- 财务报表域（F2）：净资产快照（三张表曲线数据源）
USE star_site;

CREATE TABLE IF NOT EXISTS `finance_net_worth_snapshot` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `family_id`       BIGINT UNSIGNED NOT NULL COMMENT '家庭id(冗余)',
  `owner_member_id` BIGINT UNSIGNED NOT NULL COMMENT '持有账号id',
  `snap_date`       DATE            NOT NULL COMMENT '快照日期',
  `total_asset`     DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '总资产(账户+档案资产估值)',
  `total_liability` DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '总负债',
  `net_worth`       DECIMAL(18,2)   NOT NULL DEFAULT 0.00 COMMENT '净资产',
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_date` (`owner_member_id`, `snap_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='净资产快照';

-- 财务域（F4）：预算 + 目标
USE star_site;

-- 月度分类预算（period = YYYY-MM；同 family+category+period 唯一）
CREATE TABLE IF NOT EXISTS `finance_budget` (
  `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键(雪花id)',
  `family_id`       BIGINT UNSIGNED NOT NULL COMMENT '家庭id(冗余)',
  `owner_member_id` BIGINT UNSIGNED NOT NULL COMMENT '持有账号id',
  `category`        VARCHAR(16)     NOT NULL COMMENT '分类枚举(支出类)',
  `period`          VARCHAR(7)      NOT NULL COMMENT '预算月份 YYYY-MM',
  `amount`          DECIMAL(16,2)   NOT NULL COMMENT '预算金额',
  `deleted`         TINYINT         NOT NULL DEFAULT 0,
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_cat_period` (`owner_member_id`, `category`, `period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='月度分类预算';

-- 家庭目标（购房/教育金等；V1 手动更新进度）
CREATE TABLE IF NOT EXISTS `finance_goal` (
  `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键(雪花id)',
  `family_id`       BIGINT UNSIGNED NOT NULL COMMENT '家庭id(冗余)',
  `owner_member_id` BIGINT UNSIGNED NOT NULL COMMENT '持有账号id',
  `name`            VARCHAR(64)     NOT NULL COMMENT '目标名称',
  `target_amount`   DECIMAL(16,2)   NOT NULL COMMENT '目标金额',
  `current_amount`  DECIMAL(16,2)   NOT NULL DEFAULT 0.00 COMMENT '当前已攒',
  `deadline`        DATE                     DEFAULT NULL COMMENT '截止日',
  `remark`          VARCHAR(255)             DEFAULT '',
  `deleted`         TINYINT         NOT NULL DEFAULT 0,
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner_member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭目标';

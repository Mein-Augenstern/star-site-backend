-- 保单域（star-site-insurance-service）
USE star_site;

CREATE TABLE IF NOT EXISTS `insurance_policy` (
  -- 应用侧雪花 id（ASSIGN_ID）：事务消息需要在 insert 前拿到 id 填进事件载荷
  `id`                  BIGINT UNSIGNED NOT NULL COMMENT '主键(雪花id)',
  `family_id`           BIGINT UNSIGNED NOT NULL COMMENT '家庭id(冗余,多人协作预留)',
  `owner_member_id`     BIGINT UNSIGNED NOT NULL COMMENT '持有账号id(member服务)',
  `company`             VARCHAR(64)     NOT NULL COMMENT '保险公司',
  `product_name`        VARCHAR(128)    NOT NULL COMMENT '产品名',
  `policy_no`           VARCHAR(64)              DEFAULT '' COMMENT '保单号',
  `category`            VARCHAR(16)     NOT NULL COMMENT 'critical_illness/major_medical/accident/life/annuity/vehicle/property/other',
  `insured_member_id`   BIGINT UNSIGNED NOT NULL COMMENT '被保人id(family服务自然人档案)',
  `insured_member_name` VARCHAR(32)     NOT NULL COMMENT '被保人姓名快照(免联表)',
  `start_date`          DATE                     DEFAULT NULL COMMENT '生效日',
  `maturity_date`       DATE                     DEFAULT NULL COMMENT '满期日(NULL=终身)',
  `premium`             DECIMAL(12,2)            DEFAULT 0.00 COMMENT '年保费(趸交为总保费)',
  `payment_freq`        VARCHAR(16)     NOT NULL DEFAULT 'annual' COMMENT 'single/annual',
  `next_payment_date`   DATE                     DEFAULT NULL COMMENT '下次缴费日(提醒源)',
  `coverage_amount`     DECIMAL(14,2)            DEFAULT NULL COMMENT '保额',
  `status`              TINYINT         NOT NULL DEFAULT 1 COMMENT '1有效 2缴费中断 3已满期 4已退保',
  `file_id`             BIGINT UNSIGNED          DEFAULT NULL COMMENT '电子保单附件(file服务)',
  `remark`              VARCHAR(255)             DEFAULT '',
  `deleted`             TINYINT         NOT NULL DEFAULT 0,
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner_member_id`),
  KEY `idx_family_insured` (`family_id`, `insured_member_id`),
  KEY `idx_next_payment` (`next_payment_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保单';

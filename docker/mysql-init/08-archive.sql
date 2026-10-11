-- 家庭档案域（star-site-archive-service）：证件夹 + 房产车辆台账
USE star_site;

-- 证件（雪花 id：到期提醒事件需在 insert 前拿到 id）
CREATE TABLE IF NOT EXISTS `archive_document` (
  `id`                  BIGINT UNSIGNED NOT NULL COMMENT '主键(雪花id)',
  `family_id`           BIGINT UNSIGNED NOT NULL COMMENT '家庭id(冗余)',
  `owner_member_id`     BIGINT UNSIGNED NOT NULL COMMENT '持有账号id',
  `holder_member_id`    BIGINT UNSIGNED NOT NULL COMMENT '证件持有人id(family服务自然人)',
  `holder_member_name`  VARCHAR(32)     NOT NULL COMMENT '持有人姓名快照',
  `doc_type`            VARCHAR(16)     NOT NULL COMMENT 'id_card/hukou/property_cert/vehicle_license/diploma/marriage/birth/passport/contract/other',
  `doc_no`              VARCHAR(64)              DEFAULT '' COMMENT '证件号码',
  `issue_date`          DATE                     DEFAULT NULL COMMENT '发证日期',
  `expiry_date`         DATE                     DEFAULT NULL COMMENT '有效期至(NULL=长期,提醒源)',
  `file_id`             BIGINT UNSIGNED          DEFAULT NULL COMMENT '扫描件附件(file服务)',
  `remark`              VARCHAR(255)             DEFAULT '',
  `deleted`             TINYINT         NOT NULL DEFAULT 0,
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner_member_id`),
  KEY `idx_family_holder` (`family_id`, `holder_member_id`),
  KEY `idx_expiry` (`expiry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭证件';

-- 家庭资产（房产/车辆/存款理财台账）
CREATE TABLE IF NOT EXISTS `archive_asset` (
  -- 应用侧雪花 id（ASSIGN_ID）：年检提醒事务消息需在 insert 前拿到 id 填进事件载荷
  `id`                  BIGINT UNSIGNED NOT NULL COMMENT '主键(雪花id)',
  `family_id`           BIGINT UNSIGNED NOT NULL COMMENT '家庭id(冗余)',
  `owner_member_id`     BIGINT UNSIGNED NOT NULL COMMENT '持有账号id',
  `name`                VARCHAR(64)     NOT NULL COMMENT '资产名称',
  `asset_type`          VARCHAR(16)     NOT NULL COMMENT 'real_estate/vehicle/deposit/investment/fund/other',
  `holder_member_id`    BIGINT UNSIGNED          DEFAULT NULL COMMENT '持有人id(NULL=家庭共有)',
  `holder_member_name`  VARCHAR(32)              DEFAULT '' COMMENT '持有人姓名快照',
  `acquire_date`        DATE                     DEFAULT NULL COMMENT '购置日期',
  `current_value`       DECIMAL(14,2)            DEFAULT 0.00 COMMENT '当前估值',
  `valued_at`           DATETIME                 DEFAULT NULL COMMENT '估值时间',
  `next_inspect_date`   DATE                     DEFAULT NULL COMMENT '下次年检日期(仅车辆,提醒源)',
  `remark`              VARCHAR(255)             DEFAULT '',
  `deleted`             TINYINT         NOT NULL DEFAULT 0,
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner_member_id`),
  KEY `idx_family_type` (`family_id`, `asset_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭资产';

-- 资产估值历史（更新估值自动留痕，趋势曲线数据源）
CREATE TABLE IF NOT EXISTS `archive_asset_valuation` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `asset_id`            BIGINT UNSIGNED NOT NULL COMMENT '资产id',
  `value`               DECIMAL(14,2)   NOT NULL COMMENT '估值',
  `valued_at`           DATETIME        NOT NULL COMMENT '估值时点',
  `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_asset_time` (`asset_id`, `valued_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产估值历史';

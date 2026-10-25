-- 提醒中心域（star-site-reminder-service）
-- 注意：MySQL 容器 volume 已初始化过的话，本文件不会自动执行，需手动跑一次
USE star_site;

-- 提醒规则：由领域事件消费端 upsert，幂等键 (source, biz_type, biz_id)
-- 规则是衍生物，撤销走物理删（不留 deleted，保证唯一键干净）
CREATE TABLE IF NOT EXISTS `reminder_rule` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `family_id`       BIGINT UNSIGNED NOT NULL COMMENT '家庭id',
  `owner_member_id` BIGINT UNSIGNED NOT NULL COMMENT '归属顶梁柱账号id',
  `source`          VARCHAR(32)  NOT NULL COMMENT '来源服务,如 star-site-insurance',
  `biz_type`        VARCHAR(32)  NOT NULL COMMENT '业务类型,如 policy_payment',
  `biz_id`          BIGINT UNSIGNED NOT NULL COMMENT '业务主键',
  `title`           VARCHAR(128) NOT NULL COMMENT '提醒标题',
  `first_remind_at` DATE                 DEFAULT NULL COMMENT '首次提醒日期',
  `repeat_type`     VARCHAR(16)  NOT NULL DEFAULT 'NONE' COMMENT 'NONE/ANNUAL/MONTHLY/WEEKLY/DAILY/CUSTOM',
  `repeat_until`    DATE                 DEFAULT NULL COMMENT '重复截止日(NULL=无限)',
  `advance_days`    INT          NOT NULL DEFAULT 0 COMMENT '提前天数',
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule` (`source`, `biz_type`, `biz_id`),
  KEY `idx_owner` (`owner_member_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提醒规则';

-- 提醒实例：每日扫描从规则预生成，(rule_id, due_date) 唯一防重
CREATE TABLE IF NOT EXISTS `reminder_occurrence` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `rule_id`         BIGINT UNSIGNED NOT NULL COMMENT '规则id',
  `family_id`       BIGINT UNSIGNED NOT NULL,
  `owner_member_id` BIGINT UNSIGNED NOT NULL,
  `title`           VARCHAR(128) NOT NULL COMMENT '标题(冗余自规则,规则删后通知不丢)',
  `source`          VARCHAR(32)  NOT NULL COMMENT '来源服务(冗余,跳转用)',
  `biz_type`        VARCHAR(32)  NOT NULL,
  `biz_id`          BIGINT UNSIGNED NOT NULL,
  `due_date`        DATE         NOT NULL COMMENT '应提醒日期',
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '1待提醒 2已通知 3已完成 4已忽略',
  `notified_at`     DATETIME             DEFAULT NULL COMMENT '通知时间',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_due` (`rule_id`, `due_date`),
  KEY `idx_owner_due` (`owner_member_id`, `due_date`, `status`),
  KEY `idx_due_status` (`due_date`, `status`) COMMENT '每日扫描 job：到期未通知（不带 owner/rule 前缀时兜底）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提醒实例';

-- 站内通知：到期实例的投递记录，前端铃铛数据源
CREATE TABLE IF NOT EXISTS `notification` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `occurrence_id`   BIGINT UNSIGNED NOT NULL COMMENT '提醒实例id',
  `owner_member_id` BIGINT UNSIGNED NOT NULL COMMENT '接收账号id',
  `title`           VARCHAR(128) NOT NULL COMMENT '通知标题',
  `read_flag`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner_read` (`owner_member_id`, `read_flag`),
  KEY `idx_owner_id` (`owner_member_id`, `id`) COMMENT '铃铛分页：按持有人 id 倒序（idx_owner_read 中间隔 read_flag 断序）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内通知';

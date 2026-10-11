-- 财务域（star-site-finance-service）F1：资金账户 + 交易流水
USE star_site;

-- 资金账户（钱在哪；credit/loan 为负债账户）
CREATE TABLE IF NOT EXISTS `finance_account` (
  `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键(雪花id)',
  `family_id`       BIGINT UNSIGNED NOT NULL COMMENT '家庭id(冗余,多人协作预留)',
  `owner_member_id` BIGINT UNSIGNED NOT NULL COMMENT '持有账号id',
  `name`            VARCHAR(64)  NOT NULL COMMENT '账户名称',
  `account_type`    VARCHAR(16)  NOT NULL COMMENT 'cash/bank/wechat/alipay/broker/fund/credit/loan',
  `balance`         DECIMAL(16,2) NOT NULL DEFAULT 0.00 COMMENT '当前余额(流水增量维护)',
  `initial_balance` DECIMAL(16,2)          DEFAULT 0.00 COMMENT '期初余额(建户时的钱)',
  `liability_flag`  TINYINT      NOT NULL DEFAULT 0 COMMENT '0资产账户 1负债账户',
  `institution`     VARCHAR(64)           DEFAULT '' COMMENT '开户机构',
  `remark`          VARCHAR(255)          DEFAULT '',
  `deleted`         TINYINT      NOT NULL DEFAULT 0,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner_member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资金账户';

-- 交易流水（唯一事实源；余额快照 balance_after 用于对账）
-- direction: 1收入 2支出 3转账 4调整(余额校准,不计收支)
CREATE TABLE IF NOT EXISTS `finance_transaction` (
  `id`                BIGINT UNSIGNED NOT NULL COMMENT '主键(雪花id)',
  `family_id`         BIGINT UNSIGNED NOT NULL COMMENT '家庭id(冗余)',
  `owner_member_id`   BIGINT UNSIGNED NOT NULL COMMENT '持有账号id',
  `account_id`        BIGINT UNSIGNED NOT NULL COMMENT '资金账户id',
  `account_name`      VARCHAR(64)     NOT NULL COMMENT '账户名快照(免联表)',
  `direction`         TINYINT         NOT NULL COMMENT '1收入 2支出 3转账 4调整',
  `amount`            DECIMAL(16,2)   NOT NULL COMMENT '金额(调整方向可为负)',
  `category`          VARCHAR(16)              DEFAULT NULL COMMENT '分类(枚举,调整/转账可空)',
  `category_name`     VARCHAR(32)              DEFAULT '' COMMENT '分类名快照',
  `trans_time`        DATETIME        NOT NULL COMMENT '交易时间',
  `counterparty`      VARCHAR(64)              DEFAULT '' COMMENT '对手方',
  `transfer_group_id` BIGINT UNSIGNED          DEFAULT NULL COMMENT '转账组id(出+入两笔共享)',
  `balance_after`     DECIMAL(16,2)            DEFAULT NULL COMMENT '记账后余额快照(对账用)',
  `source`            TINYINT         NOT NULL DEFAULT 1 COMMENT '1手记 2微信导入 3支付宝导入',
  `external_id`       VARCHAR(64)              DEFAULT NULL COMMENT '账单方交易号(与source组成唯一键,导入幂等)',
  `file_id`           BIGINT UNSIGNED          DEFAULT NULL COMMENT '凭证附件(file服务)',
  `remark`            VARCHAR(255)             DEFAULT '',
  `deleted`           TINYINT         NOT NULL DEFAULT 0,
  `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_family_time` (`family_id`, `trans_time`),
  KEY `idx_account_time` (`account_id`, `trans_time`),
  UNIQUE KEY `uk_owner_source_ext` (`owner_member_id`, `source`, `external_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易流水';

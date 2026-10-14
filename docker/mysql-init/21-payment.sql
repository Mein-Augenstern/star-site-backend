-- 支付域（star-site-platform-payment-service）
-- 注意：MySQL 容器 volume 已初始化过的话，本文件不会自动执行，需手动跑一次
USE star_site;

-- 支付单：统一收银台主体表。
-- 幂等三键：uk_order_no（应用侧单号）、uk_biz（source+biz_type+biz_id 业务在途唯一）、
-- uk_channel_trade（channel+channel_trade_no 渠道回调防重放）
CREATE TABLE IF NOT EXISTS `payment_order` (
  `id`               BIGINT UNSIGNED NOT NULL COMMENT '主键(雪花id,应用侧赋值)',
  `order_no`         VARCHAR(32)  NOT NULL COMMENT '支付单号(PP+雪花,渠道out_trade_no)',
  `source`           VARCHAR(32)  NOT NULL COMMENT '来源服务,如 star-site-biz-insurance',
  `biz_type`         VARCHAR(32)  NOT NULL COMMENT '业务类型,如 insurance_policy',
  `biz_id`           BIGINT UNSIGNED NOT NULL COMMENT '业务主键',
  `family_id`        BIGINT UNSIGNED      DEFAULT NULL COMMENT '家庭id(业务透传冗余)',
  `owner_member_id`  BIGINT UNSIGNED NOT NULL COMMENT '归属账号id(支付人)',
  `channel`          VARCHAR(16)  NOT NULL COMMENT '支付渠道:mock/alipay/wechat',
  `subject`          VARCHAR(128) NOT NULL COMMENT '商品标题',
  `amount`           DECIMAL(12,2) NOT NULL COMMENT '支付金额(元)',
  `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '1已创建 2成功 3已关闭 4已退款',
  `channel_trade_no` VARCHAR(64)          DEFAULT NULL COMMENT '渠道侧交易号(回调回填)',
  `paid_at`          DATETIME             DEFAULT NULL COMMENT '支付成功时间',
  `charge_payload`   VARCHAR(512)         DEFAULT NULL COMMENT '支付凭证(二维码内容/链接/沙箱token)',
  `remark`           VARCHAR(255)         DEFAULT NULL COMMENT '备注',
  `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常 1已删',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_biz` (`source`, `biz_type`, `biz_id`),
  UNIQUE KEY `uk_channel_trade` (`channel`, `channel_trade_no`),
  KEY `idx_owner` (`owner_member_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付单';

-- 退款单：对支付成功支付单的部分/全额退款 attempt
CREATE TABLE IF NOT EXISTS `payment_refund` (
  `id`                BIGINT UNSIGNED NOT NULL COMMENT '主键(雪花id,应用侧赋值)',
  `refund_no`         VARCHAR(32)  NOT NULL COMMENT '退款单号(RF+雪花,渠道out_refund_no)',
  `order_id`          BIGINT UNSIGNED NOT NULL COMMENT '支付单id',
  `order_no`          VARCHAR(32)  NOT NULL COMMENT '支付单号(冗余)',
  `channel`           VARCHAR(16)  NOT NULL COMMENT '支付渠道(随支付单冗余)',
  `channel_trade_no`  VARCHAR(64)          DEFAULT NULL COMMENT '渠道侧原交易号',
  `channel_refund_no` VARCHAR(64)          DEFAULT NULL COMMENT '渠道侧退款单号',
  `amount`            DECIMAL(12,2) NOT NULL COMMENT '退款金额(元)',
  `status`            TINYINT      NOT NULL DEFAULT 1 COMMENT '1已受理 2成功 3失败',
  `reason`            VARCHAR(255)         DEFAULT NULL COMMENT '退款原因',
  `operator_member_id` BIGINT UNSIGNED NOT NULL COMMENT '操作账号id',
  `refunded_at`       DATETIME             DEFAULT NULL COMMENT '退款成功时间',
  `deleted`           TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常 1已删',
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  UNIQUE KEY `uk_channel_refund` (`channel`, `channel_refund_no`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款单';

-- 多端统一账号体系：身份表（账号-身份分离，经典 multi-auth 模型）
USE star_site;

CREATE TABLE IF NOT EXISTS `member_identity` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `member_id`    BIGINT UNSIGNED NOT NULL COMMENT '账号id',
  `identity_type` VARCHAR(16) NOT NULL COMMENT 'WECHAT_MP/ALIPAY_MP/PASSWORD/PHONE/EMAIL',
  `identifier`   VARCHAR(128) NOT NULL COMMENT 'openid/unionid/用户名/手机号/邮箱',
  `credential`   VARCHAR(128) DEFAULT NULL COMMENT 'bcrypt密码(密码身份)/session_key(微信系,敏感不下发)',
  `unionid`      VARCHAR(64) DEFAULT NULL COMMENT '微信系跨端归一键',
  `deleted`      TINYINT NOT NULL DEFAULT 0,
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_identifier` (`identity_type`, `identifier`),
  KEY `idx_member` (`member_id`),
  KEY `idx_unionid` (`unionid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录身份';

-- member 表放开用户名/密码非空约束（微信静默注册用户可以两者皆无）
ALTER TABLE `member` MODIFY `username` VARCHAR(32) DEFAULT NULL COMMENT '用户名(密码登录身份,微信用户可为空)',
                     MODIFY `password` VARCHAR(80) DEFAULT NULL COMMENT '兼容保留,权威在 member_identity';

-- 存量迁移：每个有用户名密码的账号补一条 PASSWORD 身份
INSERT INTO `member_identity` (`member_id`, `identity_type`, `identifier`, `credential`)
SELECT m.`id`, 'PASSWORD', m.`username`, m.`password`
FROM `member` m
WHERE m.`deleted` = 0 AND m.`username` IS NOT NULL AND m.`password` IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM `member_identity` i WHERE i.`member_id` = m.`id` AND i.`identity_type` = 'PASSWORD');

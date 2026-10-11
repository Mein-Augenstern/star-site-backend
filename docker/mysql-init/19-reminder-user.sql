-- 自建提醒规则：reminder_rule 增加归属成员列（用户自建规则挂在家庭成员上）
USE star_site;

ALTER TABLE `reminder_rule`
  ADD COLUMN `family_member_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '提醒归属成员(用户自建规则;业务规则为空)' AFTER `family_id`,
  ADD KEY `idx_family_member` (`family_member_id`);

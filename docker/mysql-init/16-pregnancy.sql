-- 孕期档案（孕妇角色：孕周计算/产检计划/体重管理锚点）
USE star_site;

CREATE TABLE IF NOT EXISTS `pregnancy_profile` (
  `id`                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `family_member_id`    BIGINT UNSIGNED NOT NULL COMMENT '家庭成员id(唯一)',
  `lmp_date`            DATE NOT NULL COMMENT '末次月经第一天',
  `height_cm`           DECIMAL(5,1) DEFAULT NULL COMMENT '身高cm(算孕前BMI)',
  `pre_pregnancy_weight` DECIMAL(6,2) DEFAULT NULL COMMENT '孕前体重kg',
  `ended`               TINYINT NOT NULL DEFAULT 0 COMMENT '0孕期中 1已结束(产后)',
  `remark`              VARCHAR(255) DEFAULT '',
  `deleted`             TINYINT NOT NULL DEFAULT 0,
  `created_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member` (`family_member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='孕期档案';

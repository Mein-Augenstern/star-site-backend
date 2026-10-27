-- ============================================================
-- 2026-10-24 索引修复迁移（存量环境手工执行；新环境经 07/09 init 文件自动生效）
-- 来源：全量 SQL/索引审计（三组件 + star 系列 12 服务 + Python 服务）
-- 全部为 ADD INDEX（在线 DDL，INPLACE），无数据变更
-- ============================================================
USE star_site;

-- 【高危1】finance_transaction 流水主查询：所有查询按 owner_member_id 过滤，
-- idx_family_time 最左列 family_id 被跳过 → 每次分页全表扫+filesort
ALTER TABLE `finance_transaction`
  ADD INDEX `idx_owner_time` (`owner_member_id`, `trans_time`, `id`);

-- 【高危2】finance_transaction 转账删除路径：transfer_group_id 无索引 → 全表扫
ALTER TABLE `finance_transaction`
  ADD INDEX `idx_transfer_group` (`transfer_group_id`);

-- 【高危3】reminder_occurrence 每日扫描 job：status+due_date 查询两个现有索引最左前缀均不命中
ALTER TABLE `reminder_occurrence`
  ADD INDEX `idx_due_status` (`due_date`, `status`);

-- 【高危4】notification 铃铛分页：idx_owner_read(owner, read_flag) 中间隔 read_flag，
-- ORDER BY id DESC 无法顺索引序 → 回表+filesort，随通知量增长恶化
ALTER TABLE `notification`
  ADD INDEX `idx_owner_id` (`owner_member_id`, `id`);

-- 【组件】async_command 认领取回：claim_token 无索引（两版 DDL 均缺）；
-- 注意：非唯一索引——一批认领共用同一 token
ALTER TABLE `async_command`
  ADD INDEX `idx_claim_token` (`claim_token`);

-- 【组件】sys_sequence 表形态升级：id 物理主键（__global__ 行固定 id=1）+ service_name 唯一键。
-- 存量表需带数据迁移：先补 id 列（__global__ 行置 1，其余行用雪花值回填），再收紧主键。
ALTER TABLE `sys_sequence`
  ADD COLUMN `id` BIGINT NOT NULL DEFAULT 0 FIRST,
  ADD COLUMN `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `instance`,
  ADD COLUMN `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP AFTER `gmt_create`;
UPDATE `sys_sequence` SET `id` = 1 WHERE `service_name` = '__global__' AND `id` = 0;
-- 非 __global__ 存量行（各服务审计行，每服务一行）：以任意互不冲突的雪花值回填后执行：
--   ALTER TABLE `sys_sequence` ADD PRIMARY KEY (`id`),
--     ADD UNIQUE KEY `uk_sys_sequence_service_name` (`service_name`),
--     DROP COLUMN `updated_at`;

-- 【冗余清理】审计确认无任何查询引用（写放大纯开销），确认无外部报表依赖后删除
-- ALTER TABLE `insurance_policy` DROP INDEX `idx_next_payment`;
-- ALTER TABLE `archive_document` DROP INDEX `idx_expiry`;

-- 【2026-10-27 补充】锁组件表更名（历史默认 component_schedule_lock → starsite_lock）：
-- 锁行是瞬态状态，直接 RENAME 保留现网即可；不迁移则组件会在新表名下自动建表（锁状态零损失）
-- RENAME TABLE `component_schedule_lock` TO `starsite_lock`;

-- 【2026-10-28 补充】锁重入计数存储端化（hold_count 列）——重入层数从 Pod 本地移入锁行，
-- 同 ownerId 跨实例共享一致；已更名的表直接加列（未更名的先执行上方 RENAME）
-- ALTER TABLE `starsite_lock` ADD COLUMN `hold_count` INT NOT NULL DEFAULT 0
--   COMMENT '重入持有层数（存储端权威）' AFTER `locked_until`;

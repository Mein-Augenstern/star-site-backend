-- AI 解读（F2 批次二）：体检报告解读结果落库（避免重复调用 LLM 花钱）
USE star_site;

ALTER TABLE `health_report`
  ADD COLUMN `interpretation` TEXT COMMENT 'AI 解读文本' AFTER `remark`,
  ADD COLUMN `interpreted_at` DATETIME COMMENT '解读时间(未解读为 NULL)';

-- 全局 ID 组件：雪花机器位分配序列表（common/id/SequenceWorkerIdAllocator）
-- 分配语义：workerId 从 __global__ 全局池计数行原子自增取出（全平台唯一）；
-- 其余行是各服务实例的占用记录（审计可见，不参与分配）。
-- 注意：MySQL 容器 volume 已初始化过的话，本文件不会自动执行（组件也会自动建表 + 幂等 seed，双保险）
USE star_site;

CREATE TABLE IF NOT EXISTS `sys_sequence` (
  `service_name` VARCHAR(64) NOT NULL COMMENT '__global__=全局池计数行，其余为实例占用记录',
  `worker_id`    INT NOT NULL DEFAULT 0 COMMENT '该行当前的机器位（全局池行=已分配到的最大值）',
  `instance`     VARCHAR(128) DEFAULT NULL COMMENT '最近占用实例标识(服务@pid@启动戳)',
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`service_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='雪花 worker-id 分配序列表';

-- 全局池计数行（幂等 seed）
INSERT IGNORE INTO `sys_sequence` (`service_name`, `worker_id`, `instance`)
VALUES ('__global__', 0, 'pool');

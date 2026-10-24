package com.xp.starsite.common.id;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 雪花机器位分配器（sequence 表方式）：以数据库 sequence 表为底层，为每个服务实例
 * 原子分配全局唯一的雪花 workerId——workerId 从 <b>全局池</b>（__global__ 计数行）
 * 原子自增取出，全平台绝不重复；每个服务的实例标识另行记录在各自的 sequence 行
 * （谁在何时占用过哪个机器位，审计可见）。
 * <p>
 * 分配语义（原子，无锁）：
 * <pre>UPDATE ... SET worker_id = LAST_INSERT_ID(worker_id + 1) WHERE service_name = '__global__'</pre>
 * 首实例得 0，其后每实例递增；超过 1023（雪花机器位上限）回绕为 0 并大声告警
 * （回绕窗口内与存活旧实例同 id 的概率 = 同毫秒同序列，可忽略但必须可见）。
 * 全部 SQL 参数绑定（军规）；建表自动执行（IF NOT EXISTS，增量安全）。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
public class SequenceWorkerIdAllocator {

    /** 雪花机器位上限（10 位机器位：0~1023，与 IdGenerator 对齐） */
    public static final long MAX_WORKER_ID = 1023;

    /** 全局池计数行（workerId 从这一行原子自增取出，全平台唯一） */
    public static final String GLOBAL_POOL_KEY = "__global__";

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS sys_sequence (
              service_name VARCHAR(64) NOT NULL COMMENT '服务名（__global__=全局池计数行，其余为实例占用记录）',
              worker_id    INT NOT NULL DEFAULT 0 COMMENT '该行当前的机器位（全局池行=已分配到的最大值）',
              instance     VARCHAR(128) DEFAULT NULL COMMENT '最近占用实例标识(服务@pid@启动戳)',
              updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (service_name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='雪花 worker-id 分配序列表'""";

    private final DataSource dataSource;
    private final String serviceName;
    private final String instanceTag;

    public SequenceWorkerIdAllocator(DataSource dataSource, String serviceName, String instanceTag) {
        this.dataSource = dataSource;
        this.serviceName = serviceName;
        this.instanceTag = instanceTag;
    }

    /**
     * 分配雪花 workerId（全局池原子自增；sequence 表缺失自动建表）
     *
     * @return 分配到的机器位（0~1023，全平台唯一）
     * @throws SQLException 表创建/分配 SQL 失败
     */
    public long allocate() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            ensureTable(conn);
            long workerId = takeFromGlobalPool(conn);
            recordOwnership(conn);
            if (workerId > MAX_WORKER_ID) {
                // 回绕：雪花机器位只有 10 位。旧实例若已死亡则无碰撞；存活实例同 id 的
                // 碰撞需要「同毫秒 + 同序列」同时命中，概率可忽略但必须可见
                log.warn("[id] workerId 回绕: service={} 分配值 {} 超上限 {}，重置全局池为 0",
                        serviceName, workerId, MAX_WORKER_ID);
                resetGlobalPool(conn);
                return 0;
            }
            return workerId;
        }
    }

    private void ensureTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(DDL);
            // 全局池计数行（幂等）
            try (PreparedStatement seed = conn.prepareStatement(
                    "INSERT IGNORE INTO sys_sequence (service_name, worker_id, instance) VALUES (?, 0, ?)")) {
                seed.setString(1, GLOBAL_POOL_KEY);
                seed.setString(2, "pool");
                seed.executeUpdate();
            }
        }
    }

    /** 原子自增取出全局唯一机器位：LAST_INSERT_ID(expr) 在 UPDATE 命中时把自增结果带回会话 */
    private long takeFromGlobalPool(Connection conn) throws SQLException {
        try (PreparedStatement update = conn.prepareStatement(
                "UPDATE sys_sequence SET worker_id = LAST_INSERT_ID(worker_id + 1), "
                        + "instance = ?, updated_at = NOW() WHERE service_name = ?");
             PreparedStatement select = conn.prepareStatement("SELECT LAST_INSERT_ID()")) {
            update.setString(1, instanceTag);
            update.setString(2, GLOBAL_POOL_KEY);
            update.executeUpdate();
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("LAST_INSERT_ID() 无返回");
                }
                return rs.getLong(1);
            }
        }
    }

    /** 记录服务实例的占用（各自 sequence 行，审计可见；不影响分配） */
    private void recordOwnership(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO sys_sequence (service_name, worker_id, instance, updated_at) "
                        + "VALUES (?, 0, ?, NOW()) "
                        + "ON DUPLICATE KEY UPDATE instance = VALUES(instance), updated_at = NOW()")) {
            stmt.setString(1, serviceName);
            stmt.setString(2, instanceTag);
            stmt.executeUpdate();
        }
    }

    private void resetGlobalPool(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE sys_sequence SET worker_id = 0, instance = ? WHERE service_name = ?")) {
            stmt.setString(1, instanceTag);
            stmt.setString(2, GLOBAL_POOL_KEY);
            stmt.executeUpdate();
        }
    }
}

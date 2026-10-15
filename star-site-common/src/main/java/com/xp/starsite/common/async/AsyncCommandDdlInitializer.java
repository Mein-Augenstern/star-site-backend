package com.xp.starsite.common.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * star-async 建表初始化：async_command 表随组件走（每服务独立 DB 后各服务自持自己的表，
 * 不再依赖 member/共享库的表），启动时自动执行 IF NOT EXISTS 建表（增量安全）。
 * 正式 DDL 同步在 docker/mysql-init/18-async-command.sql。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
public class AsyncCommandDdlInitializer implements InitializingBean {

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS async_command (
              id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
              cmd_type    VARCHAR(64)  NOT NULL COMMENT '命令类型=执行器路由键',
              biz_key     VARCHAR(128) DEFAULT '' COMMENT '业务键(幂等/追踪)',
              payload     TEXT         DEFAULT NULL COMMENT '命令载荷 JSON',
              status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/DEAD',
              max_retry   INT          NOT NULL DEFAULT 5,
              retry_count INT          NOT NULL DEFAULT 0,
              next_run_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '到期时间(延迟队列锚点)',
              last_error  VARCHAR(500) DEFAULT NULL,
              owner       VARCHAR(64)  DEFAULT '' COMMENT '提交方服务名',
              claim_token VARCHAR(64)  DEFAULT NULL COMMENT '调度认领令牌',
              created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
              updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              KEY idx_status_next (status, next_run_at),
              KEY idx_type_biz (cmd_type, biz_key)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='异步命令'""";

    private final DataSource dataSource;

    public AsyncCommandDdlInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        if (dataSource == null) {
            log.warn("[async] 无可用 DataSource，跳过 async_command 建表检查");
            return;
        }
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(DDL);
            log.info("[async] async_command 表就绪（IF NOT EXISTS）");
        } catch (Exception e) {
            // 建表失败不阻断启动（表可能由 DBA/迁移脚本管理），首条命令提交时即会暴露
            log.warn("[async] async_command 建表检查失败（不阻断启动）: {}", e.getMessage());
        }
    }
}

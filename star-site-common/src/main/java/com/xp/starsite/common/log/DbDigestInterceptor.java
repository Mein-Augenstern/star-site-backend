package com.xp.starsite.common.log;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;

import java.sql.Statement;
import java.util.Properties;

/**
 * SQL 执行留痕拦截器（db-digest）：所有经 MyBatis 的 SQL 一条一行摘要。
 * 记录 SQL（截断 500 字符）与耗时；慢 SQL（&gt;1s）额外在业务日志 warn 提示。
 * 由 {@link StarSiteLogConfig} 注册为 mybatis 插件，全服务自动生效。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query",
                args = {Statement.class, ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update",
                args = {Statement.class})
})
public class DbDigestInterceptor implements Interceptor {

    /** 慢 SQL 阈值（ms）：超阈值在业务日志 warn */
    private static final long SLOW_SQL_MS = 1000;

    /** digest 里 SQL 最大长度（防大 SQL 刷爆日志） */
    private static final int MAX_SQL_LENGTH = 500;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = handler.getBoundSql();
        String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();
        long start = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            DigestLoggers.emit(DigestLoggers.DB, DigestLoggers.fields(
                    "method", invocation.getMethod().getName(),
                    "costMs", cost,
                    "sql", sql.length() > MAX_SQL_LENGTH ? sql.substring(0, MAX_SQL_LENGTH) + "...(" + sql.length() + ")" : sql));
            if (cost > SLOW_SQL_MS) {
                org.slf4j.LoggerFactory.getLogger("STAR_SLOW_SQL")
                        .warn("慢 SQL {}ms: {}", cost, sql);
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return target instanceof StatementHandler ? Plugin.wrap(target, this) : target;
    }

    @Override
    public void setProperties(Properties properties) {
        // 无配置项
    }
}

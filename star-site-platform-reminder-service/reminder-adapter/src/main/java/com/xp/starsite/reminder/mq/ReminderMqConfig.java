package com.xp.starsite.reminder.mq;

import com.xp.starsite.reminder.service.ReminderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * MQ 消费者条件装配：配置 rocketmq.name-server 时才注册 TOPIC_REMINDER 消费者。
 * 健康聚焦阶段 MQ 停用（yml 注释 rocketmq 段）→ 本配置不生效 → 降级纯 @Scheduled 扫描。
 * 必须条件装配：@RocketMQMessageListener 的 BeanPostProcessor 是无条件的，
 * MQ 停用时若仍注册消费者会"connect to null"导致启动失败。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Configuration
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class ReminderMqConfig {

    @Bean
    public ReminderEventConsumer reminderEventConsumer(JdbcTemplate jdbcTemplate,
                                                       ReminderService reminderService) {
        return new ReminderEventConsumer(jdbcTemplate, reminderService);
    }
}

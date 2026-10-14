package com.xp.starsite.reminder.notify;

import com.xp.starsite.api.client.MemberApi;
import com.xp.starsite.api.dto.MemberContactDTO;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.rpc.RpcCallGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 邮件触达渠道（条件装配：容器里有 JavaMailSender 才启用）。
 * 收件人取 member 档案邮箱，为空则跳过；发送失败仅告警不阻断（站内信始终是主渠道）。
 * 双保险：SMTP host 未配置时本渠道整体静默。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Component
@ConditionalOnBean(JavaMailSender.class)
public class EmailChannel {

    private static final String FROM_NAME = "StarSite 家庭管家";

    private final JavaMailSender mailSender;
    private final MemberApi memberApi;
    /** 无状态门卫实例化即用（构造器签名保持稳定，不新增注入参数） */
    private final RpcCallGuard rpc = new RpcCallGuard();
    private final String fromUser;
    private final boolean enabled;

    public EmailChannel(JavaMailSender mailSender, MemberApi memberApi,
                        @Value("${spring.mail.username:}") String fromUser,
                        @Value("${spring.mail.host:}") String host) {
        this.mailSender = mailSender;
        this.memberApi = memberApi;
        this.fromUser = fromUser;
        this.enabled = host != null && !host.isBlank();
        if (!enabled) {
            log.info("邮件触达渠道未配置 spring.mail.host，已禁用（站内信不受影响）");
        }
    }

    /** 是否具备给该用户发邮件的条件 */
    public boolean applicable(Long memberId) {
        return enabled && contactOf(memberId)
                .map(c -> c.getEmail() != null && !c.getEmail().isBlank())
                .orElse(false);
    }

    /** 发送提醒邮件；任何异常吞掉（不影响站内信主流程） */
    public void send(Long memberId, String title, String dueDate) {
        if (!enabled) {
            return;
        }
        contactOf(memberId).ifPresent(contact -> {
            if (contact.getEmail() == null || contact.getEmail().isBlank()) {
                return;
            }
            try {
                var msg = mailSender.createMimeMessage();
                var helper = new MimeMessageHelper(msg, false, "UTF-8");
                helper.setFrom(fromUser.isBlank() ? contact.getEmail() : fromUser, FROM_NAME);
                helper.setTo(contact.getEmail());
                helper.setSubject("【StarSite】" + title);
                helper.setText("""
                        您有一项家庭事项需要关注：%s
                        应办理日期：%s

                        请登录 StarSite 家庭管家查看详情。
                        """.formatted(title, dueDate));
                mailSender.send(msg);
                log.info("提醒邮件已发送: to={}, title={}", contact.getEmail(), title);
            } catch (Exception e) {
                log.warn("提醒邮件发送失败（不影响站内信）: to={}, err={}", contact.getEmail(), e.getMessage());
            }
        });
    }

    private Optional<MemberContactDTO> contactOf(Long memberId) {
        // 统一经 RpcCallGuard（判 code/异常翻译）；本渠道整体 tolerant，失败降级为无联系方式
        try {
            MemberContactDTO contact = rpc.call("reminder.member.contact", () -> memberApi.contact(memberId));
            return contact == null ? Optional.empty() : Optional.of(contact);
        } catch (Exception e) {
            log.warn("取会员联系方式失败: memberId={}, err={}", memberId, e.getMessage());
            return Optional.empty();
        }
    }
}

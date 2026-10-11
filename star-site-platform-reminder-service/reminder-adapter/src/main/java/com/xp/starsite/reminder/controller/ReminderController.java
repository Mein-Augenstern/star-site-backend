package com.xp.starsite.reminder.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.reminder.dto.UserRuleCreateDTO;
import com.xp.starsite.reminder.service.ReminderService;
import com.xp.starsite.reminder.vo.NotificationVO;
import com.xp.starsite.reminder.vo.OccurrenceVO;
import com.xp.starsite.reminder.vo.UserRuleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提醒接口：待办看板、站内通知、用户自建提醒规则。
 * 全部端点需 JWT；数据按当前账号隔离；
 * 业务逻辑统一经模板包裹（动作名 reminder.*），
 * 模板包掉成功与失败的全部响应处理，Controller 一行返回。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/reminder")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 未来 N 天要办的事（看板/提醒页）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param days     展望天数
     * @return 提醒实例列表（含已通知/待提醒，倒序）
     */
    @GetMapping("/upcoming")
    public R<List<OccurrenceVO>> upcoming(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                          @RequestParam(defaultValue = "30") int days) {
        return queryTemplate.query("reminder.upcoming",
                () -> reminderService.upcoming(memberId, days));
    }

    /**
     * 未读通知数（铃铛角标）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 未读通知条数
     */
    @GetMapping("/notifications/unread-count")
    public R<Long> unreadCount(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query("reminder.notification.unreadCount",
                () -> reminderService.unreadCount(memberId));
    }

    /**
     * 通知分页列表
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param page     页码（从 1 起）
     * @param size     每页条数
     * @return 通知列表（按时间倒序）
     */
    @GetMapping("/notifications")
    public R<List<NotificationVO>> notifications(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        return queryTemplate.query("reminder.notification.list",
                () -> reminderService.notifications(memberId, page, size));
    }

    /**
     * 标记通知已读
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param id       通知 id
     * @return 空响应
     */
    @PutMapping("/notifications/{id}/read")
    public R<Void> markRead(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                            @PathVariable Long id) {
        return writeTemplate.write("reminder.notification.read",
                () -> reminderService.markRead(memberId, id));
    }

    /**
     * 完成待办事项
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param id       提醒实例 id
     * @return 空响应
     */
    @PutMapping("/occurrences/{id}/done")
    public R<Void> done(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                        @PathVariable Long id) {
        return writeTemplate.write("reminder.occurrence.done",
                () -> reminderService.completeOccurrence(memberId, id));
    }

    /**
     * 忽略待办事项
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param id       提醒实例 id
     * @return 空响应
     */
    @PutMapping("/occurrences/{id}/dismiss")
    public R<Void> dismiss(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                           @PathVariable Long id) {
        return writeTemplate.write("reminder.occurrence.dismiss",
                () -> reminderService.dismissOccurrence(memberId, id));
    }

    /**
     * 新建自建提醒（用药/复查等；归属校验后立即补生成 30 天实例）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      规则创建参数
     * @return 新建规则视图（含待办实例数）
     */
    @PostMapping("/rules")
    public R<UserRuleVO> createUserRule(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                        @Valid @RequestBody UserRuleCreateDTO dto) {
        return writeTemplate.write("reminder.rule.create",
                () -> reminderService.createUserRule(memberId, dto));
    }

    /**
     * 我的自建提醒规则列表
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 规则列表（含待办实例数）
     */
    @GetMapping("/rules")
    public R<List<UserRuleVO>> listUserRules(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query("reminder.rule.list",
                () -> reminderService.listUserRules(memberId));
    }

    /**
     * 删除自建提醒（连带清未完成实例）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param id       规则 id
     * @return 空响应
     */
    @DeleteMapping("/rules/{id}")
    public R<Void> deleteUserRule(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                  @PathVariable Long id) {
        return writeTemplate.write("reminder.rule.delete",
                () -> reminderService.deleteUserRule(memberId, id));
    }
}

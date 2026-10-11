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
 * 业务逻辑统一经模板包裹（动作名 reminder.*）。
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

    /** 未来 N 天要办的事（看板/提醒页） */
    @GetMapping("/upcoming")
    public R<List<OccurrenceVO>> upcoming(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                          @RequestParam(defaultValue = "30") int days) {
        return R.ok(queryTemplate.execute("reminder.upcoming",
                () -> reminderService.upcoming(memberId, days)));
    }

    /** 未读通知数（铃铛角标） */
    @GetMapping("/notifications/unread-count")
    public R<Long> unreadCount(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(queryTemplate.execute("reminder.notification.unreadCount",
                () -> reminderService.unreadCount(memberId)));
    }

    /** 通知分页列表 */
    @GetMapping("/notifications")
    public R<List<NotificationVO>> notifications(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        return R.ok(queryTemplate.execute("reminder.notification.list",
                () -> reminderService.notifications(memberId, page, size)));
    }

    /** 标记通知已读 */
    @PutMapping("/notifications/{id}/read")
    public R<Void> markRead(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                            @PathVariable Long id) {
        writeTemplate.execute("reminder.notification.read",
                () -> reminderService.markRead(memberId, id));
        return R.ok();
    }

    /** 完成待办事项 */
    @PutMapping("/occurrences/{id}/done")
    public R<Void> done(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                        @PathVariable Long id) {
        writeTemplate.execute("reminder.occurrence.done",
                () -> reminderService.completeOccurrence(memberId, id));
        return R.ok();
    }

    /** 忽略待办事项 */
    @PutMapping("/occurrences/{id}/dismiss")
    public R<Void> dismiss(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                           @PathVariable Long id) {
        writeTemplate.execute("reminder.occurrence.dismiss",
                () -> reminderService.dismissOccurrence(memberId, id));
        return R.ok();
    }

    /** 新建自建提醒（用药/复查等；归属校验后立即补生成 30 天实例） */
    @PostMapping("/rules")
    public R<UserRuleVO> createUserRule(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                        @Valid @RequestBody UserRuleCreateDTO dto) {
        return R.ok(writeTemplate.execute("reminder.rule.create",
                () -> reminderService.createUserRule(memberId, dto)));
    }

    /** 我的自建提醒规则列表 */
    @GetMapping("/rules")
    public R<List<UserRuleVO>> listUserRules(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return R.ok(queryTemplate.execute("reminder.rule.list",
                () -> reminderService.listUserRules(memberId)));
    }

    /** 删除自建提醒（连带清未完成实例） */
    @DeleteMapping("/rules/{id}")
    public R<Void> deleteUserRule(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                  @PathVariable Long id) {
        writeTemplate.execute("reminder.rule.delete",
                () -> reminderService.deleteUserRule(memberId, id));
        return R.ok();
    }
}

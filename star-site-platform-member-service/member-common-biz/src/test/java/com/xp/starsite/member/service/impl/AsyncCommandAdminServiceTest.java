package com.xp.starsite.member.service.impl;

import com.xp.starsite.common.async.AsyncCommand;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.member.entity.Member;
import com.xp.starsite.member.repository.AsyncCommandAdminRepository;
import com.xp.starsite.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 异步命令管控服务单测：管理员白名单/账号角色校验、监控组装、
 * 重试/重占/终止状态机（mock 全部依赖，不连库、不启 Spring）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class AsyncCommandAdminServiceTest {

    private static final long ADMIN = 7L;
    private static final long OUTSIDER = 8L;

    @Mock
    private AsyncCommandAdminRepository adminRepository;
    @Mock
    private MemberRepository memberRepository;

    private AsyncCommandAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AsyncCommandAdminServiceImpl(adminRepository, memberRepository);
        ReflectionTestUtils.setField(service, "adminMemberIds", ADMIN + ", 9");
    }

    private Member activeMember(long id) {
        Member m = new Member();
        m.setId(id);
        m.setStatus(1);
        return m;
    }

    private AsyncCommand command(long id, String status) {
        AsyncCommand c = new AsyncCommand();
        c.setId(id);
        c.setStatus(status);
        c.setRetryCount(2);
        c.setClaimToken("token-1");
        return c;
    }

    /* ===== 管理员校验 ===== */

    @Test
    void blankWhitelistAllowsAnyLoggedInMemberWithoutRoleQuery() {
        ReflectionTestUtils.setField(service, "adminMemberIds", " ");

        Map<String, Object> out = service.summary(OUTSIDER);

        assertNotNull(out);
        verify(memberRepository, never()).findById(any());
    }

    @Test
    void whitelistMissThrows403BeforeCommandLookup() {
        BizException e = assertThrows(BizException.class, () -> service.retry(OUTSIDER, 404L));
        assertEquals(403, e.getCode());
        assertEquals("无管理员权限", e.getMessage());
        // 越权请求连命令存在性都不该查（防 id 枚举）
        verify(adminRepository, never()).findById(any());
    }

    @Test
    void whitelistHitLoadsRoleFromMemberTable() {
        when(memberRepository.findById(ADMIN)).thenReturn(Optional.of(activeMember(ADMIN)));
        when(adminRepository.countByStatus("PENDING")).thenReturn(1L);
        when(adminRepository.countByStatus("RUNNING")).thenReturn(2L);
        when(adminRepository.countByStatus("SUCCESS")).thenReturn(3L);
        when(adminRepository.countByStatus("DEAD")).thenReturn(4L);
        when(adminRepository.countBacklogDue(any(LocalDateTime.class))).thenReturn(5L);
        when(adminRepository.recent(500)).thenReturn(List.of());

        service.summary(ADMIN);

        verify(memberRepository).findById(ADMIN);
    }

    @Test
    void whitelistHitButMemberDeletedThrows403() {
        when(memberRepository.findById(ADMIN)).thenReturn(Optional.empty());

        BizException e = assertThrows(BizException.class, () -> service.list(ADMIN, null, null, 1, 20));
        assertEquals(403, e.getCode());
    }

    @Test
    void whitelistHitButMemberDisabledThrows403() {
        Member disabled = activeMember(ADMIN);
        disabled.setStatus(0);
        when(memberRepository.findById(ADMIN)).thenReturn(Optional.of(disabled));

        BizException e = assertThrows(BizException.class, () -> service.kill(ADMIN, 1L));
        assertEquals(403, e.getCode());
    }

    /* ===== 监控总览与分页 ===== */

    @Test
    void summaryAssemblesCountsBacklogAndTypeDistribution() {
        stubAdminOk();
        when(adminRepository.countByStatus("PENDING")).thenReturn(1L);
        when(adminRepository.countByStatus("RUNNING")).thenReturn(2L);
        when(adminRepository.countByStatus("SUCCESS")).thenReturn(3L);
        when(adminRepository.countByStatus("DEAD")).thenReturn(4L);
        when(adminRepository.countBacklogDue(any(LocalDateTime.class))).thenReturn(5L);
        AsyncCommand a = command(1L, "DEAD");
        a.setCmdType("bill.import");
        AsyncCommand b = command(2L, "DEAD");
        b.setCmdType("bill.import");
        AsyncCommand c = command(3L, "PENDING");
        c.setCmdType("reminder.push");
        when(adminRepository.recent(500)).thenReturn(List.of(a, b, c));

        Map<String, Object> out = service.summary(ADMIN);

        assertEquals(1L, out.get("pending"));
        assertEquals(2L, out.get("running"));
        assertEquals(3L, out.get("success"));
        assertEquals(4L, out.get("dead"));
        assertEquals(5L, out.get("backlogDue"));
        assertEquals(Map.of("bill.import", 2L, "reminder.push", 1L), out.get("byType"));
    }

    @Test
    void listSkipsPageQueryWhenTotalZero() {
        stubAdminOk();
        when(adminRepository.countPage("DEAD", null)).thenReturn(0L);

        Map<String, Object> out = service.list(ADMIN, "DEAD", null, 1, 20);

        assertEquals(0L, out.get("total"));
        assertEquals(List.of(), out.get("records"));
        assertEquals(1L, out.get("page"));
        assertEquals(20L, out.get("size"));
        verify(adminRepository, never()).page(any(), any(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void listComputesOffsetFromPage() {
        stubAdminOk();
        when(adminRepository.countPage(null, null)).thenReturn(25L);
        when(adminRepository.page(null, null, 20L, 10L)).thenReturn(List.of());

        Map<String, Object> out = service.list(ADMIN, null, null, 3, 10);

        assertEquals(25L, out.get("total"));
        verify(adminRepository).page(null, null, 20L, 10L);
    }

    /* ===== 重试/重占/终止状态机 ===== */

    @Test
    void retryRejectsRunningCommand() {
        stubAdminOk();
        when(adminRepository.findById(1L)).thenReturn(Optional.of(command(1L, "RUNNING")));

        BizException e = assertThrows(BizException.class, () -> service.retry(ADMIN, 1L));
        assertEquals(400, e.getCode());
        verify(adminRepository, never()).updateState(any());
    }

    @Test
    void retryRejectsSuccessCommand() {
        stubAdminOk();
        when(adminRepository.findById(1L)).thenReturn(Optional.of(command(1L, "SUCCESS")));

        assertThrows(BizException.class, () -> service.retry(ADMIN, 1L));
        verify(adminRepository, never()).updateState(any());
    }

    @Test
    void retryResetsDeadCommandToPending() {
        stubAdminOk();
        when(adminRepository.findById(1L)).thenReturn(Optional.of(command(1L, "DEAD")));

        service.retry(ADMIN, 1L);

        ArgumentCaptor<AsyncCommand> cap = ArgumentCaptor.forClass(AsyncCommand.class);
        verify(adminRepository).updateState(cap.capture());
        assertEquals("PENDING", cap.getValue().getStatus());
        assertEquals(0, cap.getValue().getRetryCount());
        assertNull(cap.getValue().getClaimToken());
        assertNotNull(cap.getValue().getNextRunAt());
    }

    @Test
    void reclaimRejectsNonRunningCommand() {
        stubAdminOk();
        when(adminRepository.findById(1L)).thenReturn(Optional.of(command(1L, "PENDING")));

        BizException e = assertThrows(BizException.class, () -> service.reclaim(ADMIN, 1L));
        assertEquals(400, e.getCode());
        assertEquals("仅 RUNNING 状态可重占", e.getMessage());
    }

    @Test
    void reclaimPutsStuckRunningBackToQueue() {
        stubAdminOk();
        when(adminRepository.findById(1L)).thenReturn(Optional.of(command(1L, "RUNNING")));

        service.reclaim(ADMIN, 1L);

        ArgumentCaptor<AsyncCommand> cap = ArgumentCaptor.forClass(AsyncCommand.class);
        verify(adminRepository).updateState(cap.capture());
        assertEquals("PENDING", cap.getValue().getStatus());
        assertNull(cap.getValue().getClaimToken());
    }

    @Test
    void killRejectsSuccessCommand() {
        stubAdminOk();
        when(adminRepository.findById(1L)).thenReturn(Optional.of(command(1L, "SUCCESS")));

        assertThrows(BizException.class, () -> service.kill(ADMIN, 1L));
        verify(adminRepository, never()).updateState(any());
    }

    @Test
    void killMarksDeadWithAuditableError() {
        stubAdminOk();
        when(adminRepository.findById(1L)).thenReturn(Optional.of(command(1L, "PENDING")));

        service.kill(ADMIN, 1L);

        ArgumentCaptor<AsyncCommand> cap = ArgumentCaptor.forClass(AsyncCommand.class);
        verify(adminRepository).updateState(cap.capture());
        assertEquals("DEAD", cap.getValue().getStatus());
        assertTrue(cap.getValue().getLastError().contains("MANUAL_KILL by member " + ADMIN));
    }

    @Test
    void missingCommandThrows404AfterAdminCheck() {
        stubAdminOk();
        when(adminRepository.findById(99L)).thenReturn(Optional.empty());

        BizException e = assertThrows(BizException.class, () -> service.retry(ADMIN, 99L));
        assertEquals(404, e.getCode());
        assertEquals("命令不存在", e.getMessage());
    }

    private void stubAdminOk() {
        when(memberRepository.findById(ADMIN)).thenReturn(Optional.of(activeMember(ADMIN)));
    }
}

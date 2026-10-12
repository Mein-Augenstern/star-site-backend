package com.xp.starsite.member.service.impl;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.util.JwtUtil;
import com.xp.starsite.member.dto.ProfileCompleteDTO;
import com.xp.starsite.member.entity.Member;
import com.xp.starsite.member.entity.MemberIdentity;
import com.xp.starsite.member.mapper.MemberIdentityMapper;
import com.xp.starsite.member.mapper.MemberMapper;
import com.xp.starsite.member.vo.IdentityVO;
import com.xp.starsite.member.vo.MemberVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会员身份/资料用例单测（MultiAuthController 下沉逻辑）：
 * 登录方式脱敏列表、解绑防自锁、资料完善非空字段语义、邮箱验证码绑定（mock 全部依赖）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceImplIdentityTest {

    private static final long ME = 7L;

    @Mock
    private MemberMapper memberMapper;
    @Mock
    private MemberIdentityMapper identityMapper;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private MemberServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MemberServiceImpl(memberMapper, identityMapper, jwtUtil, passwordEncoder);
    }

    @AfterEach
    void cleanCodeStore() {
        MemberServiceImpl.CodeStore.remove("EMAIL:" + ME);
    }

    private Member member(long id) {
        Member m = new Member();
        m.setId(id);
        m.setNickname("旧昵称");
        return m;
    }

    private MemberIdentity identity(long id, long memberId, String type, String identifier) {
        MemberIdentity i = new MemberIdentity();
        i.setId(id);
        i.setMemberId(memberId);
        i.setIdentityType(type);
        i.setIdentifier(identifier);
        i.setCreatedAt(LocalDateTime.of(2026, 10, 11, 8, 0));
        return i;
    }

    /* ===== 我的登录方式列表 ===== */

    @Test
    void listIdentitiesMasksIdentifierButKeepsPasswordPlain() {
        when(identityMapper.selectByMemberId(ME)).thenReturn(List.of(
                identity(1L, ME, "PASSWORD", "tom"),
                identity(2L, ME, "EMAIL", "tom@example.com")));

        List<IdentityVO> out = service.listIdentities(ME);

        assertEquals(2, out.size());
        // PASSWORD 明文回显（前端改密要用）
        assertEquals("tom", out.get(0).identifier());
        // EMAIL 中段打码
        assertEquals("to****om", out.get(1).identifier());
        assertEquals("2026-10-11T08:00", out.get(1).createdAt());
    }

    /* ===== 解绑防自锁 ===== */

    @Test
    void unbindForeignOrMissingIdentityThrows404() {
        when(identityMapper.selectById(5L)).thenReturn(null);

        BizException e = assertThrows(BizException.class, () -> service.unbindIdentity(ME, 5L));
        assertEquals(404, e.getCode());
        verify(identityMapper, never()).markDeleted(any());
    }

    @Test
    void unbindLastIdentityRejected() {
        when(identityMapper.selectById(5L)).thenReturn(identity(5L, ME, "EMAIL", "tom@example.com"));
        when(identityMapper.countByMemberId(ME)).thenReturn(1L);

        BizException e = assertThrows(BizException.class, () -> service.unbindIdentity(ME, 5L));
        assertEquals(400, e.getCode());
        assertEquals("至少保留一种登录方式，无法解绑", e.getMessage());
    }

    @Test
    void unbindMarksDeletedWhenMoreThanOneLeft() {
        when(identityMapper.selectById(5L)).thenReturn(identity(5L, ME, "EMAIL", "tom@example.com"));
        when(identityMapper.countByMemberId(ME)).thenReturn(2L);

        service.unbindIdentity(ME, 5L);

        verify(identityMapper).markDeleted(5L);
    }

    /* ===== 资料完善（非空字段才生效） ===== */

    @Test
    void completeProfileSkipsBlankNicknameButAppliesAvatar() {
        when(memberMapper.selectById(ME)).thenReturn(member(ME));
        ProfileCompleteDTO dto = new ProfileCompleteDTO();
        dto.setNickname("   ");
        dto.setAvatar("https://cdn/a.png");

        MemberVO vo = service.completeProfile(ME, dto);

        assertEquals("旧昵称", vo.getNickname());
        ArgumentCaptor<Member> cap = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper).updateProfile(cap.capture());
        assertEquals("旧昵称", cap.getValue().getNickname());
        assertEquals("https://cdn/a.png", cap.getValue().getAvatar());
    }

    @Test
    void completeProfileMissingAccountThrows404() {
        when(memberMapper.selectById(ME)).thenReturn(null);

        BizException e = assertThrows(BizException.class,
                () -> service.completeProfile(ME, new ProfileCompleteDTO()));
        assertEquals(404, e.getCode());
        assertEquals("账号不存在", e.getMessage());
    }

    /* ===== 邮箱验证码绑定 ===== */

    @Test
    void sendEmailCodeRejectsBadFormat() {
        BizException e = assertThrows(BizException.class, () -> service.sendEmailCode(ME, "not-an-email"));
        assertEquals(400, e.getCode());
        assertEquals("邮箱格式不正确", e.getMessage());
    }

    @Test
    void sendEmailCodeRejectsAlreadyBoundEmail() {
        when(identityMapper.countByIdentity("EMAIL", "tom@example.com")).thenReturn(1L);

        BizException e = assertThrows(BizException.class, () -> service.sendEmailCode(ME, "tom@example.com"));
        assertEquals(400, e.getCode());
        assertEquals("该邮箱已被其他账号绑定", e.getMessage());
    }

    @Test
    void sendEmailCodeStoresCodeUnderMemberKey() {
        when(identityMapper.countByIdentity("EMAIL", "tom@example.com")).thenReturn(0L);

        service.sendEmailCode(ME, "tom@example.com");

        // 验证码以 email:code 形式存入（5 分钟 TTL）；发送通道开发态返回 false 不影响有效性
        String saved = MemberServiceImpl.CodeStore.get("EMAIL:" + ME);
        assertEquals("tom@example.com:", saved == null ? null : saved.substring(0, saved.indexOf(':') + 1));
    }

    @Test
    void bindEmailRejectsWrongOrExpiredCode() {
        BizException e = assertThrows(BizException.class,
                () -> service.bindEmail(ME, "tom@example.com", "000000"));
        assertEquals(400, e.getCode());
        assertEquals("验证码错误或已过期", e.getMessage());
        verify(identityMapper, never()).insert(any());
    }

    @Test
    void bindEmailRejectsAlreadyBoundEmailAfterCodeCheck() {
        MemberServiceImpl.CodeStore.put("EMAIL:" + ME, "tom@example.com:123456", 300);
        when(identityMapper.countByIdentity("EMAIL", "tom@example.com")).thenReturn(1L);

        BizException e = assertThrows(BizException.class,
                () -> service.bindEmail(ME, "tom@example.com", "123456"));
        assertEquals(400, e.getCode());
        verify(identityMapper, never()).insert(any());
    }

    @Test
    void bindEmailInsertsIdentityBackfillsEmptyEmailAndConsumesCode() {
        MemberServiceImpl.CodeStore.put("EMAIL:" + ME, "tom@example.com:123456", 300);
        when(identityMapper.countByIdentity("EMAIL", "tom@example.com")).thenReturn(0L);
        Member m = member(ME);
        when(memberMapper.selectById(ME)).thenReturn(m);

        service.bindEmail(ME, "tom@example.com", "123456");

        ArgumentCaptor<MemberIdentity> cap = ArgumentCaptor.forClass(MemberIdentity.class);
        verify(identityMapper).insert(cap.capture());
        assertEquals(ME, cap.getValue().getMemberId());
        assertEquals("EMAIL", cap.getValue().getIdentityType());
        assertEquals("tom@example.com", cap.getValue().getIdentifier());
        // 账号邮箱为空 → 回填
        assertEquals("tom@example.com", m.getEmail());
        verify(memberMapper).updateProfile(m);
        // 验证码一次性：绑定后销毁
        assertNull(MemberServiceImpl.CodeStore.get("EMAIL:" + ME));
    }

    @Test
    void bindEmailKeepsExistingEmailUnchanged() {
        MemberServiceImpl.CodeStore.put("EMAIL:" + ME, "new@example.com:123456", 300);
        when(identityMapper.countByIdentity("EMAIL", "new@example.com")).thenReturn(0L);
        Member m = member(ME);
        m.setEmail("old@example.com");
        when(memberMapper.selectById(ME)).thenReturn(m);

        service.bindEmail(ME, "new@example.com", "123456");

        assertEquals("old@example.com", m.getEmail());
        verify(memberMapper, never()).updateProfile(any());
    }
}

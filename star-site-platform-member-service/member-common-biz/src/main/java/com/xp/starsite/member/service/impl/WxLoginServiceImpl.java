package com.xp.starsite.member.service.impl;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.util.JwtUtil;
import com.xp.starsite.member.constant.Gender;
import com.xp.starsite.member.constant.IdentityType;
import com.xp.starsite.member.constant.MemberStatus;
import com.xp.starsite.member.entity.Member;
import com.xp.starsite.member.entity.MemberIdentity;
import com.xp.starsite.member.integration.WxSessionGateway;
import com.xp.starsite.member.repository.MemberIdentityRepository;
import com.xp.starsite.member.repository.MemberRepository;
import com.xp.starsite.member.service.WxLoginService;
import com.xp.starsite.member.vo.LoginVO;
import com.xp.starsite.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 微信小程序登录/绑定用例（app 层：只做编排，微信协议细节在 infrastructure 的 WxSessionGateway）。
 * <p>
 * 经典时序：wx.login() code → jscode2session → openid+unionid+session_key
 * → 按 unionid 归一、openid 兜底查身份 → 命中登录 / 未命中静默注册 → 签发 JWT。
 * session_key 落 identity.credential，永不下发。
 * 数据访问经仓储层（{@link MemberRepository}/{@link MemberIdentityRepository}，实现位于 core-service），
 * 身份查询语义：openid 精确命中 findByIdentity，unionid 跨端归一 findByUnionid（均 LIMIT 1）。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxLoginServiceImpl implements WxLoginService {

    private final MemberRepository memberRepository;
    private final MemberIdentityRepository identityRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final WxSessionGateway wxSessionGateway;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO loginByWechatMp(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException(400, "code 不能为空");
        }
        WxSessionGateway.WxSession session = wxSessionGateway.fetch(code);

        // 1) openid 精确命中 → 直接登录
        MemberIdentity identity = identityRepository.findByIdentity(IdentityType.WECHAT_MP, session.openid()).orElse(null);
        // 2) unionid 跨端归一：同主体其他端（公众号/PC扫码）已注册过 → 认回同一账号
        Member member;
        if (identity != null) {
            member = memberRepository.findById(identity.getMemberId()).orElse(null);
        } else if (session.unionid() != null && !session.unionid().isBlank()) {
            MemberIdentity unionIdentity = identityRepository.findByUnionid(IdentityType.WECHAT_MP, session.unionid()).orElse(null);
            if (unionIdentity != null) {
                member = memberRepository.findById(unionIdentity.getMemberId()).orElse(null);
                identity = null; // 走"老账号新端身份"分支
            } else {
                member = null;
            }
        } else {
            member = null;
        }

        if (member == null) {
            // 3) 静默注册：微信生态经典做法，登录即有号，资料后补
            member = new Member();
            member.setUsername(null);
            member.setPassword(null);
            member.setNickname("微信用户" + session.openid().substring(Math.max(0, session.openid().length() - 6)));
            member.setGender(Gender.UNKNOWN);
            member.setStatus(MemberStatus.ACTIVE);
            memberRepository.save(member);
            log.info("微信静默注册: memberId={}", member.getId());
        }

        // 刷新/补建本端身份（session_key 敏感，仅落库 credential 列）
        if (identity == null) {
            identity = new MemberIdentity();
            identity.setMemberId(member.getId());
            identity.setIdentityType(IdentityType.WECHAT_MP);
            identity.setIdentifier(session.openid());
            identity.setUnionid(session.unionid());
            identity.setCredential(session.sessionKey());
            identityRepository.save(identity);
        } else {
            identity.setCredential(session.sessionKey());
            if (session.unionid() != null && identity.getUnionid() == null) {
                identity.setUnionid(session.unionid());
            }
            identityRepository.update(identity);
        }

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BizException(403, "账号已被禁用");
        }
        String token = jwtUtil.generateToken(member.getId(),
                member.getUsername() != null ? member.getUsername() : "wx-" + session.openid());
        return LoginVO.builder()
                .token(token)
                .expiresIn(jwtUtil.getExpireMinutes())
                .member(toVO(member))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO bindAccountByPassword(String code, String username, String password) {
        if (code == null || code.isBlank()) {
            throw new BizException(400, "code 不能为空");
        }
        WxSessionGateway.WxSession session = wxSessionGateway.fetch(code);

        // 1) 账密校验目标账号（先取目标，避免下面的比较对不存在用户 NPE）
        Member target = memberRepository.findByUsername(username).orElse(null);
        if (target == null || !passwordEncoder.matches(password, target.getPassword())) {
            throw new BizException(400, "账号或密码错误");
        }
        if (target.getStatus() != MemberStatus.ACTIVE) {
            throw new BizException(403, "账号已被禁用");
        }

        // 2) openid 已绑其他账号 → 拒绝重复绑定；已绑同一账号 → 幂等返回
        MemberIdentity existing = identityRepository.findByIdentity(IdentityType.WECHAT_MP, session.openid()).orElse(null);
        if (existing != null && !existing.getMemberId().equals(target.getId())) {
            throw new BizException(400, "该微信已绑定其他账号，请先在原账号解绑");
        }
        if (existing != null && existing.getMemberId().equals(target.getId())) {
            String token = jwtUtil.generateToken(target.getId(),
                    target.getUsername() != null ? target.getUsername() : "wx-bound");
            return LoginVO.builder()
                    .token(token)
                    .expiresIn(jwtUtil.getExpireMinutes())
                    .member(toVO(target))
                    .build();
        }

        // 3) openid 挂到目标账号（已有 WECHAT_MP 身份则更新，否则新建）
        MemberIdentity identity = existing;
        if (identity == null) {
            identity = new MemberIdentity();
            identity.setIdentityType(IdentityType.WECHAT_MP);
            identity.setIdentifier(session.openid());
            identity.setMemberId(target.getId());
            identity.setUnionid(session.unionid());
            identity.setCredential(session.sessionKey());
            identityRepository.save(identity);
        } else {
            identity.setMemberId(target.getId());
            identity.setCredential(session.sessionKey());
            identityRepository.update(identity);
        }
        String token = jwtUtil.generateToken(target.getId(),
                target.getUsername() != null ? target.getUsername() : "wx-" + session.openid());
        log.info("微信身份绑定账号: target={}", target.getUsername());
        return LoginVO.builder()
                .token(token)
                .expiresIn(jwtUtil.getExpireMinutes())
                .member(toVO(target))
                .build();
    }

    private MemberVO toVO(Member m) {
        MemberVO vo = new MemberVO();
        org.springframework.beans.BeanUtils.copyProperties(m, vo);
        return vo;
    }
}

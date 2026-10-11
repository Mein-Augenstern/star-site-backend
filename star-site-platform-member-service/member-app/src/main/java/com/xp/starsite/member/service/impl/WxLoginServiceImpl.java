package com.xp.starsite.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.util.JwtUtil;
import com.xp.starsite.member.entity.Member;
import com.xp.starsite.member.entity.MemberIdentity;
import com.xp.starsite.member.integration.WxSessionGateway;
import com.xp.starsite.member.mapper.MemberIdentityMapper;
import com.xp.starsite.member.mapper.MemberMapper;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxLoginServiceImpl implements WxLoginService {

    private final MemberMapper memberMapper;
    private final MemberIdentityMapper identityMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final WxSessionGateway wxSessionGateway;

    @Override
    public LoginVO loginByWechatMp(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException(400, "code 不能为空");
        }
        WxSessionGateway.WxSession session = wxSessionGateway.fetch(code);

        // 1) openid 精确命中 → 直接登录
        MemberIdentity identity = identityMapper.selectOne(new LambdaQueryWrapper<MemberIdentity>()
                .eq(MemberIdentity::getIdentityType, "WECHAT_MP")
                .eq(MemberIdentity::getIdentifier, session.openid()));
        // 2) unionid 跨端归一：同主体其他端（公众号/PC扫码）已注册过 → 认回同一账号
        Member member;
        if (identity != null) {
            member = memberMapper.selectById(identity.getMemberId());
        } else if (session.unionid() != null && !session.unionid().isBlank()) {
            MemberIdentity unionIdentity = identityMapper.selectOne(new LambdaQueryWrapper<MemberIdentity>()
                    .eq(MemberIdentity::getIdentityType, "WECHAT_MP")
                    .eq(MemberIdentity::getUnionid, session.unionid())
                    .last("LIMIT 1"));
            if (unionIdentity != null) {
                member = memberMapper.selectById(unionIdentity.getMemberId());
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
            member.setGender(0);
            member.setStatus(1);
            memberMapper.insert(member);
            log.info("微信静默注册: memberId={}", member.getId());
        }

        // 刷新/补建本端身份（session_key 敏感，仅落库）
        if (identity == null) {
            identity = new MemberIdentity();
            identity.setMemberId(member.getId());
            identity.setIdentityType("WECHAT_MP");
            identity.setIdentifier(session.openid());
            identity.setUnionid(session.unionid());
            identity.setCredential(session.sessionKey());
            identityMapper.insert(identity);
        } else {
            identity.setCredential(session.sessionKey());
            if (session.unionid() != null && identity.getUnionid() == null) {
                identity.setUnionid(session.unionid());
            }
            identityMapper.updateById(identity);
        }

        if (member.getStatus() != 1) {
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
    @Transactional
    public LoginVO bindAccountByPassword(String code, String username, String password) {
        if (code == null || code.isBlank()) {
            throw new BizException(400, "code 不能为空");
        }
        WxSessionGateway.WxSession session = wxSessionGateway.fetch(code);

        // 1) openid 已绑其他账号 → 拒绝重复绑定
        MemberIdentity existing = identityMapper.selectOne(new LambdaQueryWrapper<MemberIdentity>()
                .eq(MemberIdentity::getIdentityType, "WECHAT_MP")
                .eq(MemberIdentity::getIdentifier, session.openid()));
        if (existing != null && !existing.getMemberId().equals(loadByUsername(username).getId())) {
            throw new BizException(400, "该微信已绑定其他账号，请先在原账号解绑");
        }
        if (existing != null && existing.getMemberId().equals(loadByUsername(username).getId())) {
            // 已绑同一账号：幂等返回
            Member m = loadByUsername(username);
            String token = jwtUtil.generateToken(m.getId(), m.getUsername() != null ? m.getUsername() : "wx-bound");
            return LoginVO.builder()
                    .token(token)
                    .expiresIn(jwtUtil.getExpireMinutes())
                    .member(toVO(m))
                    .build();
        }

        // 2) 账密校验目标账号
        Member target = loadByUsername(username);
        if (target == null || !passwordEncoder.matches(password, target.getPassword())) {
            throw new BizException(400, "账号或密码错误");
        }
        if (target.getStatus() != 1) {
            throw new BizException(403, "账号已被禁用");
        }

        // 3) openid 挂到目标账号（已有 WECHAT_MP 身份则更新，否则新建）
        MemberIdentity identity = identityMapper.selectOne(new LambdaQueryWrapper<MemberIdentity>()
                .eq(MemberIdentity::getIdentityType, "WECHAT_MP")
                .eq(MemberIdentity::getIdentifier, session.openid()));
        if (identity == null) {
            identity = new MemberIdentity();
            identity.setIdentityType("WECHAT_MP");
            identity.setIdentifier(session.openid());
            identity.setMemberId(target.getId());
            identity.setUnionid(session.unionid());
            identity.setCredential(session.sessionKey());
            identityMapper.insert(identity);
        } else {
            identity.setMemberId(target.getId());
            identity.setCredential(session.sessionKey());
            identityMapper.updateById(identity);
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

    private Member loadByUsername(String username) {
        return memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getUsername, username));
    }

    private MemberVO toVO(Member m) {
        MemberVO vo = new MemberVO();
        org.springframework.beans.BeanUtils.copyProperties(m, vo);
        return vo;
    }
}

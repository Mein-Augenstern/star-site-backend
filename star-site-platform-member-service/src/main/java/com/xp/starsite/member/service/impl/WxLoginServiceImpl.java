package com.xp.starsite.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.util.JwtUtil;
import com.xp.starsite.member.entity.Member;
import com.xp.starsite.member.entity.MemberIdentity;
import com.xp.starsite.member.mapper.MemberIdentityMapper;
import com.xp.starsite.member.mapper.MemberMapper;
import com.xp.starsite.member.service.WxLoginService;
import com.xp.starsite.member.vo.LoginVO;
import com.xp.starsite.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * 微信小程序登录实现。
 * <p>
 * 经典时序：wx.login() code → jscode2session(appid,secret) → openid+unionid+session_key
 * → 按 unionid 归一、openid 兜底查身份 → 命中登录 / 未命中静默注册 → 签发 JWT。
 * <p>
 * 双通道：配置 wx.mp.secret 走真实 jscode2session；未配置走 mock（code 即 openid），
 * 开发态不依赖真 secret 即可全链路跑通。session_key 落 identity.credential，永不下发。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WxLoginServiceImpl implements WxLoginService {

    private final MemberMapper memberMapper;
    private final MemberIdentityMapper identityMapper;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Value("${wx.mp.appid:}")
    private String appid;

    @Value("${wx.mp.secret:}")
    private String secret;

    private RestClient restClient() {
        return RestClient.create();
    }

    @Override
    public LoginVO loginByWechatMp(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException(400, "code 不能为空");
        }
        WxSession session = fetchSession(code);

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
        WxSession session = fetchSession(code);

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

    private com.xp.starsite.member.entity.Member loadByUsername(String username) {
        return memberMapper.selectOne(new LambdaQueryWrapper<com.xp.starsite.member.entity.Member>()
                .eq(com.xp.starsite.member.entity.Member::getUsername, username));
    }

    private WxSession fetchSession(String code) {
        if (secret == null || secret.isBlank()) {
            // mock 通道：code 即 openid，开发态全链路不依赖真 secret
            log.info("wx-login mock 通道（未配置 wx.mp.secret）: code={}", code);
            return new WxSession(code, "sk-mock-" + code.hashCode(), null);
        }
        JsonNode resp;
        try {
            resp = restClient().get()
                    .uri("https://api.weixin.qq.com/sns/jscode2session?appid={a}&secret={s}&js_code={c}&grant_type=authorization_code",
                            appid, secret, code)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.warn("jscode2session 调用失败: {}", e.getMessage());
            throw new BizException(503, "微信登录服务暂不可用，请稍后再试");
        }
        if (resp == null || resp.path("errcode").asInt(0) != 0) {
            int err = resp == null ? -1 : resp.path("errcode").asInt(-1);
            log.warn("jscode2session 返回错误: {}", resp);
            if (err == 40029 || err == 40163) {
                throw new BizException(400, "登录凭证无效，请重试");
            }
            throw new BizException(503, "微信登录失败，请稍后再试");
        }
        return new WxSession(resp.path("openid").asText(),
                resp.path("session_key").asText(null),
                resp.hasNonNull("unionid") ? resp.path("unionid").asText() : null);
    }

    private MemberVO toVO(Member m) {
        MemberVO vo = new MemberVO();
        org.springframework.beans.BeanUtils.copyProperties(m, vo);
        return vo;
    }

    private record WxSession(String openid, String sessionKey, String unionid) {
    }
}

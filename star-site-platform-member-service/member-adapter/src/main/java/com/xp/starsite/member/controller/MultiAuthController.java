package com.xp.starsite.member.controller;

import com.xp.starsite.common.constants.AuthConstants;
import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.R;
import com.xp.starsite.common.template.QueryTemplate;
import com.xp.starsite.common.template.WriteTemplate;
import com.xp.starsite.member.dto.ProfileCompleteDTO;
import com.xp.starsite.member.entity.Member;
import com.xp.starsite.member.entity.MemberIdentity;
import com.xp.starsite.member.mapper.MemberIdentityMapper;
import com.xp.starsite.member.mapper.MemberMapper;
import com.xp.starsite.member.service.MemberService;
import com.xp.starsite.member.service.WxLoginService;
import com.xp.starsite.member.vo.LoginVO;
import com.xp.starsite.member.vo.MemberVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 多端认证接口：wx-login 免 token；identities/bind/profile 需 JWT。
 *
 * <p>会员域多登录方式入口：微信静默登录/注册、账密绑定微信、登录方式列表与解绑
 * （至少保留一种防自锁）、资料完善、邮箱验证码绑定（开发态验证码走内存+日志）。
 * 数据访问经 DAL 业务语义方法（SQL 在 mapper XML）。
 * <p>
 * 业务逻辑统一经模板包裹：模板包掉成功与失败的全部响应处理（成功 R.ok / 失败 R.fail），
 * Controller 一行返回；动作命名规范 member.auth.* / member.bind.* / member.profile.*。
 *
 * @author xiaopu
 * @since 2026-10
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MultiAuthController {

    private final WxLoginService wxLoginService;
    private final MemberIdentityMapper identityMapper;
    private final MemberMapper memberMapper;
    private final MemberService memberService;
    private final QueryTemplate queryTemplate;
    private final WriteTemplate writeTemplate;

    /**
     * 微信小程序登录：静默注册/登录（免 token）
     *
     * @param body 登录入参（wx.login 的 code）
     * @return 登录视图（JWT、有效期与会员摘要）
     */
    @PostMapping("/wx-login")
    public R<LoginVO> wxLogin(@RequestBody Map<String, String> body) {
        return writeTemplate.write("member.auth.wxLogin",
                () -> wxLoginService.loginByWechatMp(body.get("code")));
    }

    /**
     * 账密绑定微信身份：将当前微信 openid 挂到指定账号（多端同一份数据）
     *
     * @param body 绑定入参（code/username/password）
     * @return 绑定后重新签发的登录视图
     */
    @PostMapping("/bind/account")
    public R<LoginVO> bindAccount(@RequestBody Map<String, String> body) {
        return writeTemplate.write("member.bind.account", () -> wxLoginService.bindAccountByPassword(
                body.get("code"), body.get("username"), body.get("password")));
    }

    /**
     * 我的登录方式列表（标识符脱敏展示）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @return 登录方式视图列表（id/类型/脱敏标识/绑定时间）
     */
    @GetMapping("/identities")
    public R<List<IdentityVO>> identities(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId) {
        return queryTemplate.query("member.bind.list", () -> identityMapper.selectByMemberId(memberId)
                .stream().map(i -> new IdentityVO(i.getId(), i.getIdentityType(),
                        mask(i.getIdentityType(), i.getIdentifier()), i.getCreatedAt() == null ? null : i.getCreatedAt().toString()))
                .toList());
    }

    /**
     * 解绑登录方式（至少保留一种，防自锁）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param id       绑定关系 id
     * @return 解绑成功（无业务数据）
     */
    @DeleteMapping("/identities/{id}")
    public R<Void> unbind(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                          @PathVariable Long id) {
        return writeTemplate.write("member.bind.unbind", () -> {
            MemberIdentity identity = identityMapper.selectById(id);
            if (identity == null || !identity.getMemberId().equals(memberId)) {
                throw new BizException(404, "绑定关系不存在");
            }
            if (identityMapper.countByMemberId(memberId) <= 1) {
                throw new BizException(400, "至少保留一种登录方式，无法解绑");
            }
            identityMapper.markDeleted(id);
        });
    }

    /**
     * 完善昵称/头像（微信静默注册后引导补充）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param dto      资料完善内容（昵称/头像，可选字段）
     * @return 完善后的会员资料视图
     */
    @PutMapping("/profile")
    public R<MemberVO> completeProfile(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                       @Valid @RequestBody ProfileCompleteDTO dto) {
        return writeTemplate.write("member.profile.complete", () -> {
            Member member = memberMapper.selectById(memberId);
            if (member == null) {
                throw new BizException(404, "账号不存在");
            }
            if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
                member.setNickname(dto.getNickname());
            }
            if (dto.getAvatar() != null) {
                member.setAvatar(dto.getAvatar());
            }
            memberMapper.updateProfile(member);
            return memberService.getById(memberId);
        });
    }

    /**
     * 发送邮箱验证码（M1：SMTP 未配置时验证码仍有效，从日志取；验证码存内存，生产换 Redis）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param body     入参（email）
     * @return 发送受理（无业务数据）
     */
    @PostMapping("/bind/email-code")
    public R<Void> sendEmailCode(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                                 @RequestBody Map<String, String> body) {
        return writeTemplate.write("member.bind.emailCode.send", () -> {
            String email = body.get("email");
            if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new BizException(400, "邮箱格式不正确");
            }
            if (identityMapper.countByIdentity("EMAIL", email) > 0) {
                throw new BizException(400, "该邮箱已被其他账号绑定");
            }
            String code = String.valueOf(100000 + new java.security.SecureRandom().nextInt(900000));
            CodeStore.put("EMAIL:" + memberId, email + ":" + code, 300);
            boolean sent = MailSender.trySend(email, code);
            // 开发态未配 SMTP（sent=false）：验证码仍有效，提示从日志取（不发信）
        });
    }

    /**
     * 邮箱验证码绑定（校验验证码后落 EMAIL 身份；账号邮箱为空则回填）
     *
     * @param memberId 当前登录账号（JWT 注入）
     * @param body     入参（email/code）
     * @return 绑定成功（无业务数据）
     */
    @PostMapping("/bind/email")
    public R<Void> bindEmail(@RequestAttribute(AuthConstants.CLAIM_MEMBER_ID) Long memberId,
                             @RequestBody Map<String, String> body) {
        return writeTemplate.write("member.bind.email", () -> {
            String email = body.get("email");
            String code = body.get("code");
            String saved = CodeStore.get("EMAIL:" + memberId);
            if (saved == null || !saved.equals(email + ":" + code)) {
                throw new BizException(400, "验证码错误或已过期");
            }
            if (identityMapper.countByIdentity("EMAIL", email) > 0) {
                throw new BizException(400, "该邮箱已被其他账号绑定");
            }
            MemberIdentity identity = new MemberIdentity();
            identity.setMemberId(memberId);
            identity.setIdentityType("EMAIL");
            identity.setIdentifier(email);
            identityMapper.insert(identity);
            Member member = memberMapper.selectById(memberId);
            if (member != null && member.getEmail() == null) {
                member.setEmail(email);
                memberMapper.updateProfile(member);
            }
            CodeStore.remove("EMAIL:" + memberId);
        });
    }

    private String mask(String type, String id) {
        if ("PASSWORD".equals(type)) {
            return id;
        }
        if (id == null || id.length() <= 4) {
            return id;
        }
        return id.substring(0, 2) + "****" + id.substring(id.length() - 2);
    }

    public record IdentityVO(Long id, String identityType, String identifier, String createdAt) {
    }

    /** 内存验证码存储（5 分钟过期；生产换 Redis） */
    static final class CodeStore {
        private static final Map<String, String[]> STORE = new java.util.concurrent.ConcurrentHashMap<>();

        static void put(String key, String value, long ttlSeconds) {
            STORE.put(key, new String[]{value, String.valueOf(System.currentTimeMillis() + ttlSeconds * 1000)});
        }

        static String get(String key) {
            String[] v = STORE.get(key);
            if (v == null) return null;
            if (Long.parseLong(v[1]) < System.currentTimeMillis()) {
                STORE.remove(key);
                return null;
            }
            return v[0];
        }

        static void remove(String key) {
            STORE.remove(key);
        }
    }

    /** 验证码投递通道：M1 开发态打日志（生产接 SMTP/短信服务商后替换本类即可） */
    static final class MailSender {
        static boolean trySend(String to, String code) {
            org.slf4j.LoggerFactory.getLogger(MailSender.class)
                    .info("[开发态] 邮箱验证码 to={} code={}（5 分钟内有效；生产环境接入 SMTP 后真实发信）", to, code);
            return false;
        }
    }
}

package com.xp.starsite.member.service.impl;

import com.xp.starsite.common.exception.BizException;
import com.xp.starsite.common.result.ResultCode;
import com.xp.starsite.common.util.JwtUtil;
import com.xp.starsite.member.constant.Gender;
import com.xp.starsite.member.constant.MemberStatus;
import com.xp.starsite.member.dto.LoginDTO;
import com.xp.starsite.member.dto.MemberUpdateDTO;
import com.xp.starsite.member.dto.PasswordUpdateDTO;
import com.xp.starsite.member.dto.ProfileCompleteDTO;
import com.xp.starsite.member.dto.RegisterDTO;
import com.xp.starsite.member.entity.Member;
import com.xp.starsite.member.entity.MemberIdentity;
import com.xp.starsite.member.repository.MemberIdentityRepository;
import com.xp.starsite.member.repository.MemberRepository;
import com.xp.starsite.member.service.MemberService;
import com.xp.starsite.member.vo.IdentityVO;
import com.xp.starsite.member.vo.LoginVO;
import com.xp.starsite.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 会员服务实现。数据访问经仓储层（{@link MemberRepository}/{@link MemberIdentityRepository}，
 * 实现位于 core-service、底层调用 dal DAO），业务层只见领域模型、零 DAO 依赖。
 *
 * @author xiaopu
 * @since 2026-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberIdentityRepository identityRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        if (memberRepository.countByUsername(dto.getUsername()) > 0) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }
        Member member = new Member();
        member.setUsername(dto.getUsername());
        member.setPassword(passwordEncoder.encode(dto.getPassword()));
        member.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        member.setGender(Gender.UNKNOWN);
        member.setStatus(MemberStatus.ACTIVE);
        memberRepository.save(member);
        log.info("新会员注册: id={}, username={}", member.getId(), member.getUsername());
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        Member member = memberRepository.findByUsername(dto.getUsername()).orElse(null);
        if (member == null || !passwordEncoder.matches(dto.getPassword(), member.getPassword())) {
            throw new BizException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }
        String token = jwtUtil.generateToken(member.getId(), member.getUsername());
        return LoginVO.builder()
                .token(token)
                .expiresIn(jwtUtil.getExpireMinutes())
                .member(toVO(member))
                .build();
    }

    @Override
    public MemberVO getById(Long id) {
        return toVO(requireMember(id));
    }

    @Override
    public com.xp.starsite.api.dto.MemberContactDTO getContact(Long id) {
        MemberVO member = getById(id);
        if (member == null) {
            return null;
        }
        com.xp.starsite.api.dto.MemberContactDTO dto = new com.xp.starsite.api.dto.MemberContactDTO();
        dto.setId(member.getId());
        dto.setNickname(member.getNickname());
        dto.setEmail(member.getEmail());
        return dto;
    }

    @Override
    public MemberVO updateProfile(Long id, MemberUpdateDTO dto) {
        Member member = requireMember(id);
        if (dto.getNickname() != null) {
            member.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            member.setAvatar(dto.getAvatar());
        }
        if (dto.getEmail() != null) {
            member.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            member.setPhone(dto.getPhone());
        }
        if (dto.getGender() != null) {
            member.setGender(dto.getGender());
        }
        if (dto.getBio() != null) {
            member.setBio(dto.getBio());
        }
        // 非空字段才更新（动态 SQL 语义同原 MyBatis-Plus updateById）
        memberRepository.updateProfile(member);
        // 回读，拿到数据库维护的 updated_at
        return toVO(requireMember(id));
    }

    @Override
    public void changePassword(Long id, PasswordUpdateDTO dto) {
        Member member = requireMember(id);
        if (!passwordEncoder.matches(dto.getOldPassword(), member.getPassword())) {
            throw new BizException(ResultCode.OLD_PASSWORD_ERROR);
        }
        memberRepository.updatePassword(id, passwordEncoder.encode(dto.getNewPassword()));
    }

    @Override
    public List<IdentityVO> listIdentities(Long memberId) {
        return identityRepository.findByMemberId(memberId)
                .stream().map(i -> new IdentityVO(i.getId(), i.getIdentityType(),
                        mask(i.getIdentityType(), i.getIdentifier()), i.getCreatedAt() == null ? null : i.getCreatedAt().toString()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindIdentity(Long memberId, Long id) {
        MemberIdentity identity = identityRepository.findById(id).orElse(null);
        if (identity == null || !identity.getMemberId().equals(memberId)) {
            throw new BizException(404, "绑定关系不存在");
        }
        if (identityRepository.countByMemberId(memberId) <= 1) {
            throw new BizException(400, "至少保留一种登录方式，无法解绑");
        }
        identityRepository.markDeleted(id);
    }

    @Override
    public MemberVO completeProfile(Long memberId, ProfileCompleteDTO dto) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            throw new BizException(404, "账号不存在");
        }
        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            member.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            member.setAvatar(dto.getAvatar());
        }
        memberRepository.updateProfile(member);
        return getById(memberId);
    }

    @Override
    public void sendEmailCode(Long memberId, String email) {
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BizException(400, "邮箱格式不正确");
        }
        if (identityRepository.countByIdentity("EMAIL", email) > 0) {
            throw new BizException(400, "该邮箱已被其他账号绑定");
        }
        String code = String.valueOf(100000 + new java.security.SecureRandom().nextInt(900000));
        CodeStore.put("EMAIL:" + memberId, email + ":" + code, 300);
        boolean sent = MailSender.trySend(email, code);
        // 开发态未配 SMTP（sent=false）：验证码仍有效，提示从日志取（不发信）
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindEmail(Long memberId, String email, String code) {
        String saved = CodeStore.get("EMAIL:" + memberId);
        if (saved == null || !saved.equals(email + ":" + code)) {
            throw new BizException(400, "验证码错误或已过期");
        }
        if (identityRepository.countByIdentity("EMAIL", email) > 0) {
            throw new BizException(400, "该邮箱已被其他账号绑定");
        }
        MemberIdentity identity = new MemberIdentity();
        identity.setMemberId(memberId);
        identity.setIdentityType("EMAIL");
        identity.setIdentifier(email);
        identityRepository.save(identity);
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member != null && member.getEmail() == null) {
            member.setEmail(email);
            memberRepository.updateProfile(member);
        }
        CodeStore.remove("EMAIL:" + memberId);
    }

    /** 标识符脱敏（PASSWORD 明文回显——前端改密用；其余中段打码） */
    private String mask(String type, String id) {
        if ("PASSWORD".equals(type)) {
            return id;
        }
        if (id == null || id.length() <= 4) {
            return id;
        }
        return id.substring(0, 2) + "****" + id.substring(id.length() - 2);
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

    private Member requireMember(Long id) {
        Member member = memberRepository.findById(id).orElse(null);
        if (member == null) {
            throw new BizException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        return member;
    }

    private MemberVO toVO(Member member) {
        MemberVO vo = new MemberVO();
        BeanUtils.copyProperties(member, vo);
        return vo;
    }
}

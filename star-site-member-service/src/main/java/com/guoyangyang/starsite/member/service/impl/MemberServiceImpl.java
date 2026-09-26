package com.xiaopu.starsite.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaopu.starsite.common.exception.BizException;
import com.xiaopu.starsite.common.result.ResultCode;
import com.xiaopu.starsite.common.util.JwtUtil;
import com.xiaopu.starsite.member.dto.LoginDTO;
import com.xiaopu.starsite.member.dto.MemberUpdateDTO;
import com.xiaopu.starsite.member.dto.PasswordUpdateDTO;
import com.xiaopu.starsite.member.dto.RegisterDTO;
import com.xiaopu.starsite.member.entity.Member;
import com.xiaopu.starsite.member.mapper.MemberMapper;
import com.xiaopu.starsite.member.service.MemberService;
import com.xiaopu.starsite.member.vo.LoginVO;
import com.xiaopu.starsite.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 会员服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void register(RegisterDTO dto) {
        Long count = memberMapper.selectCount(
                new LambdaQueryWrapper<Member>().eq(Member::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }
        Member member = new Member();
        member.setUsername(dto.getUsername());
        member.setPassword(passwordEncoder.encode(dto.getPassword()));
        member.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        member.setGender(0);
        member.setStatus(1);
        memberMapper.insert(member);
        log.info("新会员注册: id={}, username={}", member.getId(), member.getUsername());
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        Member member = memberMapper.selectOne(
                new LambdaQueryWrapper<Member>().eq(Member::getUsername, dto.getUsername()));
        if (member == null || !passwordEncoder.matches(dto.getPassword(), member.getPassword())) {
            throw new BizException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (member.getStatus() != 1) {
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
        memberMapper.updateById(member);
        // 回读，拿到数据库维护的 updated_at
        return toVO(requireMember(id));
    }

    @Override
    public void changePassword(Long id, PasswordUpdateDTO dto) {
        Member member = requireMember(id);
        if (!passwordEncoder.matches(dto.getOldPassword(), member.getPassword())) {
            throw new BizException(ResultCode.OLD_PASSWORD_ERROR);
        }
        member.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        memberMapper.updateById(member);
    }

    private Member requireMember(Long id) {
        Member member = memberMapper.selectById(id);
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

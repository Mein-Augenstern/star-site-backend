package com.xp.starsite.member.service;

import com.xp.starsite.member.dto.LoginDTO;
import com.xp.starsite.member.dto.MemberUpdateDTO;
import com.xp.starsite.member.dto.PasswordUpdateDTO;
import com.xp.starsite.member.dto.RegisterDTO;
import com.xp.starsite.member.vo.LoginVO;
import com.xp.starsite.member.vo.MemberVO;

/**
 * 会员服务接口
 *
 * @author xiaopu
 * @since 2026-10
 */
public interface MemberService {

    /** 注册 */
    void register(RegisterDTO dto);

    /** 登录，签发 JWT */
    LoginVO login(LoginDTO dto);

    /** 查询个人资料 */
    MemberVO getById(Long id);

    /** 编辑个人资料 */
    MemberVO updateProfile(Long id, MemberUpdateDTO dto);

    /** 修改密码 */
    void changePassword(Long id, PasswordUpdateDTO dto);
}

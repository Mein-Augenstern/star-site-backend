package com.xiaopu.starsite.member.service;

import com.xiaopu.starsite.member.dto.LoginDTO;
import com.xiaopu.starsite.member.dto.MemberUpdateDTO;
import com.xiaopu.starsite.member.dto.PasswordUpdateDTO;
import com.xiaopu.starsite.member.dto.RegisterDTO;
import com.xiaopu.starsite.member.vo.LoginVO;
import com.xiaopu.starsite.member.vo.MemberVO;

/**
 * 会员服务接口
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

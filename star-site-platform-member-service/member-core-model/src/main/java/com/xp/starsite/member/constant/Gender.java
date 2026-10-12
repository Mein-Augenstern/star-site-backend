package com.xp.starsite.member.constant;

/**
 * 性别（member.gender / 家庭成员快照 gender 落库值，0未知 1男 2女）
 *
 * @author xiaopu
 * @since 2026-10
 */
public final class Gender {

    /** 未知（注册时未采集） */
    public static final int UNKNOWN = 0;

    /** 男 */
    public static final int MALE = 1;

    /** 女 */
    public static final int FEMALE = 2;

    private Gender() {
    }
}

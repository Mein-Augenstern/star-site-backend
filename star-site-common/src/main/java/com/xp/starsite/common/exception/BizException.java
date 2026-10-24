package com.xp.starsite.common.exception;

import com.xp.starsite.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：service 层抛出，由全局异常处理器转成统一响应
 *
 * @author muyi
 * @since 2026-10
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}

package com.example.medical.util;

import lombok.Getter;

/**
 * 业务异常。
 * 用于"已经发生过写操作之后才发现的失败"，抛出来才能触发 @Transactional 回滚
 * （RuntimeException 才回滚，而 return Result.error(...) 不会）。
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }

    public BizException(String msg) {
        this(400, msg);
    }
}

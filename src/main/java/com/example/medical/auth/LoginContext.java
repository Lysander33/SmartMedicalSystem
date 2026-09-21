package com.example.medical.auth;

import com.example.medical.util.BizException;

/**
 * 当前请求的登录身份，用 ThreadLocal 存放。
 * 拦截器在 preHandle 入口、afterCompletion 出口都必须 clear，
 * 否则 Tomcat 线程复用时会把上一个请求的身份带给下一个请求（串号）。
 */
public final class LoginContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 取当前身份，未登录直接抛 401 */
    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(401, "登录已失效，请重新登录");
        }
        return user;
    }

    /** 当前登录患者的 patient.id；身份不是患者时抛 403 */
    public static Integer currentPatientId() {
        LoginUser user = require();
        if (!Role.PATIENT.equals(user.getRole())) {
            throw new BizException(403, "该操作仅限患者账号");
        }
        return user.getRefId();
    }

    /** 当前登录医生的 doctor.id；身份不是医生时抛 403 */
    public static Integer currentDoctorId() {
        LoginUser user = require();
        if (!Role.DOCTOR.equals(user.getRole())) {
            throw new BizException(403, "该操作仅限医生账号");
        }
        return user.getRefId();
    }
}

package com.example.medical.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前请求的登录身份。由 AuthInterceptor 解析 token 后放进 LoginContext。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {
    /** sys_user.id */
    private Integer userId;
    /** 登录账号：患者=手机号，医生=工号 */
    private String username;
    /** patient / doctor / pharmacy / admin */
    private String role;
    /** 业务主键：patient.id 或 doctor.id；药房/管理员为 null */
    private Integer refId;
    /** 显示名称 */
    private String name;
}

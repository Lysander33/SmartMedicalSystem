package com.example.medical.entity;

import lombok.Data;

/**
 * 医生
 */
@Data
public class Doctor {
    private Integer id;
    private String name;
    private String title;
    private Integer deptId;
    private String deptName;
    private String specialty;
    private String intro;
    private Double fee;
    private Double score;
    private String onlineStatus;
    private Integer status;
    /** 登录工号，来自 sys_user.username（只读，供前端展示） */
    private String account;
    /** 仅用于管理员新增医生时传初始密码，密码实际存在 sys_user 表，doctor 表没有该列 */
    private String password;
}

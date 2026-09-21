package com.example.medical.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 统一登录账号：四个角色（患者/医生/药房/管理员）共用一张表，靠 role 区分
 */
@Data
@Schema(description = "统一登录账号")
public class SysUser {
    private Integer id;
    @Schema(description = "登录账号（患者=手机号，医生=工号）", example = "D1001")
    private String username;
    private String password;
    @Schema(description = "角色：patient/doctor/pharmacy/admin", example = "doctor")
    private String role;
    @Schema(description = "关联业务主键：patient.id 或 doctor.id；药房/管理员为 null", example = "1")
    private Integer refId;
    @Schema(description = "显示名称", example = "王建国")
    private String name;
    @Schema(description = "1=启用 0=停用（登录唯一开关）", example = "1")
    private Integer status;
    private Date createTime;
}

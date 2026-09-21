package com.example.medical.auth;

/**
 * 角色常量与各端首页。四个角色：患者 / 医生 / 药房 / 管理员。
 */
public final class Role {

    public static final String PATIENT = "patient";
    public static final String DOCTOR = "doctor";
    public static final String PHARMACY = "pharmacy";
    public static final String ADMIN = "admin";

    private Role() {
    }

    public static boolean valid(String role) {
        return PATIENT.equals(role) || DOCTOR.equals(role) || PHARMACY.equals(role) || ADMIN.equals(role);
    }

    /** 角色中文名，用于提示语 */
    public static String label(String role) {
        return switch (role == null ? "" : role) {
            case PATIENT -> "患者";
            case DOCTOR -> "医生";
            case PHARMACY -> "药房";
            case ADMIN -> "管理员";
            default -> "未知身份";
        };
    }

    /** 登录成功后跳转的首页 */
    public static String home(String role) {
        return switch (role == null ? "" : role) {
            case PATIENT -> "/";
            case DOCTOR -> "/doctor.html";
            case PHARMACY -> "/pharmacy.html";
            case ADMIN -> "/admin.html";
            default -> "/login.html";
        };
    }
}

package com.example.medical.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Date;

/**
 * 患者账号
 */
@Data
@Schema(description = "患者账号")
public class Patient {
    private Integer id;
    @Schema(description = "手机号(登录账号)", example = "13800000001")
    private String phone;
    /** 仅用于注册/登录时的请求体传参，密码实际存在 sys_user 表，patient 表没有该列 */
    @Schema(description = "登录密码（仅注册/登录传参用）", example = "123456")
    private String password;
    @Schema(description = "姓名", example = "王大明")
    private String name;
    private String idCard;
    private String sex;
    private Integer status;
    private Date createTime;
}

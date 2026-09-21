package com.example.medical.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 就诊人（一个账号可添加多名就诊人）
 */
@Data
@Schema(description = "就诊人")
public class Visitor {
    private Integer id;
    private Integer patientId;
    @Schema(description = "就诊人姓名", example = "王大明")
    private String name;
    private String sex;
    private Integer age;
    private String idCard;
    private String phone;
}

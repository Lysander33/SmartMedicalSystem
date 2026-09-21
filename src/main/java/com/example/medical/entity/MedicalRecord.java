package com.example.medical.entity;

import lombok.Data;
import java.util.Date;

/**
 * 病历（就诊记录）
 */
@Data
public class MedicalRecord {
    private Integer id;
    private Integer patientId;
    private Integer visitorId;
    private String visitorName;
    private Integer doctorId;
    private String doctorName;
    private String deptName;
    /** 门诊（挂号就诊）/ 问诊（在线咨询） */
    private String type;
    /** 来源挂号单，门诊病历有值 */
    private Integer registrationId;
    /** 来源问诊，问诊病历有值 */
    private Integer consultationId;
    private String diagnosis;
    private String advice;
    private Date createTime;
}

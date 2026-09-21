package com.example.medical.entity;

import lombok.Data;
import java.util.Date;

/**
 * 挂号记录
 */
@Data
public class Registration {
    private Integer id;
    private String regNo;
    private Integer patientId;
    private Integer visitorId;
    private String visitorName;
    private Integer doctorId;
    private String doctorName;
    private String deptName;
    private Date workDate;
    private String timeSlot;
    private Double fee;
    private String status;
    private String payType;
    private Date payTime;
    /** 就诊完成时间（医生归档门诊病历时写入） */
    private Date finishTime;
    private Date createTime;
}

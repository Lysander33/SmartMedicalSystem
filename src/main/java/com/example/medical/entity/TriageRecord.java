package com.example.medical.entity;

import lombok.Data;
import java.util.Date;

/**
 * 预分诊记录
 */
@Data
public class TriageRecord {
    private Integer id;
    private Integer patientId;
    private Integer visitorId;
    private String visitorName;
    private String bodyPart;
    private String symptoms;
    private String duration;
    private String suggestedDept;
    private String urgency;
    private String advice;
    private Date createTime;
}

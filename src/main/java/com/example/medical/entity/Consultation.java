package com.example.medical.entity;

import lombok.Data;
import java.util.Date;

/**
 * 在线咨询（问诊）
 */
@Data
public class Consultation {
    private Integer id;
    private Integer patientId;
    private Integer visitorId;
    private String visitorName;
    private Integer doctorId;
    private String doctorName;
    private String type;
    private String description;
    private String status;
    private String summary;
    private Date createTime;
    private Date finishTime;
}

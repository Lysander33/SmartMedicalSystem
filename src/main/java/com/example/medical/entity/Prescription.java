package com.example.medical.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * 电子处方
 */
@Data
public class Prescription {
    private Integer id;
    private String rxNo;
    private Integer consultationId;
    private Integer patientId;
    private String visitorName;
    private Integer doctorId;
    private String doctorName;
    private String diagnosis;
    private String status;
    private Double totalAmount;
    private String remark;
    private Date createTime;
    private Date finishTime;
    //处方明细（展示用）
    private List<PrescriptionItem> items;
}

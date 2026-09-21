package com.example.medical.entity;

import lombok.Data;
import java.util.Date;

/**
 * 医生排班号源
 */
@Data
public class Schedule {
    private Integer id;
    private Integer doctorId;
    private Date workDate;
    private String timeSlot;
    private Integer totalCount;
    private Integer remainCount;
    //展示用字段
    private String doctorName;
    private String deptName;
    private Double fee;
}

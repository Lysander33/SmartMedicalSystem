package com.example.medical.entity;

import lombok.Data;
import java.util.Date;

/**
 * 缴费订单
 */
@Data
public class     Payment {
    private Integer id;
    private String orderNo;
    private Integer patientId;
    private String bizType;
    private Integer bizId;
    private String title;
    private Double amount;
    private String status;
    private String payType;
    private Date payTime;
    private Date createTime;
    private String ticketNo;
}

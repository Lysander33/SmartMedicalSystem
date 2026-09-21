package com.example.medical.entity;

import lombok.Data;
import java.util.Date;

/**
 * 问诊消息
 */
@Data
public class Message {
    private Integer id;
    private Integer consultationId;
    private String sender;
    private String content;
    private Date createTime;
}

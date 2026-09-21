package com.example.medical.entity;

import lombok.Data;
import java.util.Date;

/**
 * 在线问答
 */
@Data
public class QaQuestion {
    private Integer id;
    private Integer patientId;
    private String patientName;
    private String content;
    private String answer;
    private String status;
    private Date createTime;
    private Date answerTime;
}

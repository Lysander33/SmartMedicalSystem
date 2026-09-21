package com.example.medical.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 开具处方的请求参数
 */
@Data
@Schema(description = "开具处方的请求参数")
public class PrescriptionRequest {
    @Schema(description = "问诊ID", required = true, example = "1")
    private Integer consultationId;
    @Schema(description = "诊断结论", example = "急性上呼吸道感染")
    private String diagnosis;
    @Schema(description = "处方药品明细")
    private List<PrescriptionItem> items;
}

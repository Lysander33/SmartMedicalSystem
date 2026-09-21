package com.example.medical.controller;

import com.example.medical.auth.LoginContext;
import com.example.medical.entity.MedicalRecord;
import com.example.medical.entity.Prescription;
import com.example.medical.mapper.MedicalRecordMapper;
import com.example.medical.mapper.PrescriptionItemMapper;
import com.example.medical.mapper.PrescriptionMapper;
import com.example.medical.util.BizException;
import com.example.medical.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 患者端-记录与报告查询接口（我的处方、病历/就诊记录）
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/records")
@Tag(name = "患者端-记录与报告查询")
public class RecordsController {

    @Autowired
    private PrescriptionMapper prescriptionMapper;
    @Autowired
    private PrescriptionItemMapper prescriptionItemMapper;
    @Autowired
    private MedicalRecordMapper medicalRecordMapper;

    /**
     * 我的处方（含药品明细与用药说明）
     */
    @Operation(summary = "我的处方（含药品明细）")
    @GetMapping("/prescriptions")
    public Result<List<Prescription>> prescriptions(@Parameter(description = "患者ID", required = true, example = "1")
                                                    @RequestParam @Positive(message = "患者ID必须为正数") Integer patientId) {
        List<Prescription> list = prescriptionMapper.selectByPatientId(requireMe(patientId));
        for (Prescription p : list) {
            p.setItems(prescriptionItemMapper.selectByPrescriptionId(p.getId()));
        }
        return Result.successList(list);
    }

    /**
     * 我的病历/就诊记录
     */
    @Operation(summary = "我的病历（就诊记录）")
    @GetMapping("/medical")
    public Result<List<MedicalRecord>> medical(@Parameter(description = "患者ID", required = true, example = "1")
                                               @RequestParam @Positive(message = "患者ID必须为正数") Integer patientId) {
        return Result.successList(medicalRecordMapper.selectByPatientId(requireMe(patientId)));
    }

    /** 只能查看本人的数据 */
    private Integer requireMe(Integer patientId) {
        Integer me = LoginContext.currentPatientId();
        if (!me.equals(patientId)) {
            throw new BizException(403, "只能查看本人的数据");
        }
        return me;
    }
}

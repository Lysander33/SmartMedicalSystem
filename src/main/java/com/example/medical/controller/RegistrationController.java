package com.example.medical.controller;

import com.example.medical.auth.LoginContext;
import com.example.medical.entity.Registration;
import com.example.medical.service.RegistrationService;
import com.example.medical.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 患者端-预约挂号接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/reg")
@RequiredArgsConstructor
@Tag(name = "患者端-预约挂号")
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * 挂号下单：选择排班号源+就诊人，生成挂号订单与待缴费单
     */
    @Operation(summary = "挂号下单")
    @PostMapping("/create")
    public Result<Registration> create(@Parameter(description = "排班ID", required = true, example = "1")
                                       @RequestParam @Positive(message = "排班ID必须为正数") Integer scheduleId,
                                       @Parameter(description = "就诊人ID", required = true, example = "1")
                                       @RequestParam @Positive(message = "就诊人ID必须为正数") Integer visitorId,
                                       @Parameter(description = "患者ID", required = true, example = "1")
                                       @RequestParam @Positive(message = "患者ID必须为正数") Integer patientId) {
        log.info("挂号下单, scheduleId={}, visitorId={}", scheduleId, visitorId);
        return Result.success(registrationService.create(scheduleId, visitorId, patientId,
                LoginContext.currentPatientId()));
    }

    /**
     * 我的挂号记录
     */
    @Operation(summary = "我的挂号记录")
    @GetMapping("/my")
    public Result<List<Registration>> my(@Parameter(description = "患者ID", required = true, example = "1")
                                         @RequestParam @Positive(message = "患者ID必须为正数") Integer patientId) {
        return Result.successList(registrationService.myList(patientId, LoginContext.currentPatientId()));
    }

    /**
     * 取消挂号（退号）：费用原路退回，号源释放
     */
    @Operation(summary = "取消挂号（退号退款）")
    @PostMapping("/cancel")
    public Result<String> cancel(@Parameter(description = "挂号记录ID", required = true, example = "1")
                                 @RequestParam @Positive(message = "挂号ID必须为正数") Integer id) {
        return Result.success(registrationService.cancel(id, LoginContext.currentPatientId()));
    }
}

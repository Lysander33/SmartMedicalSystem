package com.example.medical.controller;

import com.example.medical.auth.LoginContext;
import com.example.medical.entity.Payment;
import com.example.medical.service.PaymentService;
import com.example.medical.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 患者端-在线缴费接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
@Tag(name = "患者端-在线缴费")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 待缴清单（挂号费、药品费）
     */
    @Operation(summary = "待缴清单")
    @GetMapping("/pending")
    public Result<List<Payment>> pending(@Parameter(description = "患者ID", required = true, example = "1")
                                         @RequestParam @Positive(message = "患者ID必须为正数") Integer patientId) {
        return Result.successList(paymentService.pendingList(patientId, LoginContext.currentPatientId()));
    }

    /**
     * 在线支付：改订单状态并联动业务单据（挂号单转已支付 / 处方流转到药房）
     */
    @Operation(summary = "在线支付")
    @PostMapping("/pay")
    public Result<Payment> pay(@Parameter(description = "订单ID", required = true, example = "1")
                               @RequestParam @Positive(message = "订单ID必须为正数") Integer paymentId,
                               @Parameter(description = "支付方式", required = true, example = "微信")
                               @RequestParam @NotBlank(message = "支付方式不能为空") String payType) {
        return Result.success(paymentService.pay(paymentId, payType, LoginContext.currentPatientId()));
    }

    /**
     * 缴费记录
     */
    @Operation(summary = "缴费记录")
    @GetMapping("/records")
    public Result<List<Payment>> records(@Parameter(description = "患者ID", required = true, example = "1")
                                         @RequestParam @Positive(message = "患者ID必须为正数") Integer patientId) {
        return Result.successList(paymentService.records(patientId, LoginContext.currentPatientId()));
    }

    /**
     * 电子票夹（已支付订单的电子票据）
     */
    @Operation(summary = "电子票夹")
    @GetMapping("/tickets")
    public Result<List<Payment>> tickets(@Parameter(description = "患者ID", required = true, example = "1")
                                         @RequestParam @Positive(message = "患者ID必须为正数") Integer patientId) {
        return Result.successList(paymentService.tickets(patientId, LoginContext.currentPatientId()));
    }
}

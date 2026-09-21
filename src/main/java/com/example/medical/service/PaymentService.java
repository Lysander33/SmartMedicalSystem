package com.example.medical.service;

import com.example.medical.entity.Payment;
import com.example.medical.mapper.PaymentMapper;
import com.example.medical.mapper.PrescriptionMapper;
import com.example.medical.mapper.RegistrationMapper;
import com.example.medical.util.BizException;
import com.example.medical.util.NoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 患者端在线缴费：待缴清单、支付、缴费记录、电子票夹。
 * 登录身份由 Controller 取出后作为参数传入，本类不依赖 LoginContext，可脱离 Web 环境单测。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final RegistrationMapper registrationMapper;
    private final PrescriptionMapper prescriptionMapper;

    /**
     * 在线支付：改订单状态并联动业务单据（挂号单转已支付 / 处方流转到药房）
     */
    @Transactional
    public Payment pay(Integer paymentId, String payType, Integer operatorId) {
        log.info("在线支付, patientId={}, paymentId={}, payType={}", operatorId, paymentId, payType);
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new BizException(404, "订单不存在");
        }
        assertOwner(payment.getPatientId(), operatorId, "只能支付本人的订单");
        if (!"待支付".equals(payment.getStatus())) {
            throw new BizException(400, "订单当前状态不能支付（防止重复支付），当前状态：" + payment.getStatus());
        }
        //带前置状态条件：连点两次支付时第二次影响行数为0
        if (paymentMapper.pay(paymentId, payType, NoGenerator.ticketNo()) == 0) {
            throw new BizException(400, "该订单已支付，请勿重复操作");
        }

        //联动业务单据状态；这里是跨领域直接调 mapper（挂号/处方暂未抽 Service），
        //联动失败则整体回滚，不出现"钱付了单子没动"的情况
        if ("挂号费".equals(payment.getBizType())) {
            if (registrationMapper.pay(payment.getBizId(), payType) == 0) {
                throw new BizException(400, "挂号单状态异常（可能已取消），支付失败");
            }
        } else if ("药品费".equals(payment.getBizType())) {
            //药品费缴纳后处方流转到药房（待发药）
            if (prescriptionMapper.updateStatusFrom(payment.getBizId(), "待发药", "待缴费") == 0) {
                throw new BizException(400, "处方状态异常（可能已作废），支付失败");
            }
        }
        return paymentMapper.selectById(paymentId);
    }

    /**
     * 待缴清单（挂号费、药品费）
     */
    public List<Payment> pendingList(Integer patientId, Integer operatorId) {
        assertOwner(patientId, operatorId, "只能查看本人的数据");
        return paymentMapper.selectPendingByPatientId(patientId);
    }

    /**
     * 缴费记录
     */
    public List<Payment> records(Integer patientId, Integer operatorId) {
        assertOwner(patientId, operatorId, "只能查看本人的数据");
        return paymentMapper.selectByPatientId(patientId);
    }

    /**
     * 电子票夹（已支付订单的电子票据）
     */
    public List<Payment> tickets(Integer patientId, Integer operatorId) {
        List<Payment> all = records(patientId, operatorId);
        all.removeIf(p -> !"已支付".equals(p.getStatus()));
        return all;
    }

    /** 客户端传来的 id 不可信，必须与登录身份一致 */
    private void assertOwner(Integer ownerId, Integer operatorId, String msg) {
        if (!Objects.equals(ownerId, operatorId)) {
            throw new BizException(403, msg);
        }
    }
}

package com.example.medical.service;

import com.example.medical.entity.Payment;
import com.example.medical.entity.Registration;
import com.example.medical.entity.Schedule;
import com.example.medical.entity.Visitor;
import com.example.medical.mapper.PaymentMapper;
import com.example.medical.mapper.RegistrationMapper;
import com.example.medical.mapper.ScheduleMapper;
import com.example.medical.mapper.VisitorMapper;
import com.example.medical.util.BizException;
import com.example.medical.util.NoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 患者端预约挂号：下单、退号、查询。
 * 登录身份由 Controller 取出后作为参数传入，本类不依赖 LoginContext，可脱离 Web 环境单测。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationMapper registrationMapper;
    private final ScheduleMapper scheduleMapper;
    private final VisitorMapper visitorMapper;
    private final PaymentMapper paymentMapper;

    /**
     * 挂号下单：选择排班号源+就诊人，生成挂号订单与待缴费单
     */
    @Transactional
    public Registration create(Integer scheduleId, Integer visitorId, Integer patientId, Integer operatorId) {
        log.info("挂号下单, scheduleId={}, visitorId={}", scheduleId, visitorId);
        assertOwner(patientId, operatorId, "只能为本人账号挂号");
        //所有校验都放在扣号源之前：旧写法先扣号源再校验就诊人，
        //传一个不存在的 visitorId 会白扣号源且不生成任何单据
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BizException(404, "排班不存在");
        }
        Visitor visitor = visitorMapper.selectById(visitorId);
        if (visitor == null) {
            throw new BizException(404, "就诊人不存在");
        }
        assertOwner(visitor.getPatientId(), operatorId, "该就诊人不属于当前登录账号");
        if (registrationMapper.countConflict(visitorId, scheduleId) > 0) {
            throw new BizException(400, "该就诊人在此时段已有挂号，请勿重复挂号");
        }
        //第一个写操作：扣减号源（号源不足时影响行数为0）
        if (scheduleMapper.reduceRemain(scheduleId) == 0) {
            throw new BizException(400, "该时段号源已约满，请选择其他时段");
        }

        Registration reg = new Registration();
        reg.setRegNo(NoGenerator.regNo());
        reg.setPatientId(patientId);
        reg.setVisitorId(visitorId);
        reg.setVisitorName(visitor.getName());
        reg.setDoctorId(schedule.getDoctorId());
        reg.setDoctorName(schedule.getDoctorName());
        reg.setDeptName(schedule.getDeptName());
        reg.setWorkDate(schedule.getWorkDate());
        reg.setTimeSlot(schedule.getTimeSlot());
        reg.setFee(schedule.getFee());
        registrationMapper.insert(reg);

        //生成待缴费订单（挂号费）
        Payment payment = new Payment();
        payment.setOrderNo(NoGenerator.orderNo());
        payment.setPatientId(patientId);
        payment.setBizType("挂号费");
        payment.setBizId(reg.getId());
        payment.setTitle("挂号费-" + schedule.getDeptName() + "-" + schedule.getDoctorName()
                + " " + dateKey(schedule.getWorkDate()) + " " + schedule.getTimeSlot());
        payment.setAmount(schedule.getFee());
        paymentMapper.insert(payment);

        //insert 用 SQL 字面量写的状态，实体里补上再返回，避免响应里 status 为 null
        reg.setStatus("待支付");
        return reg;
    }

    /**
     * 取消挂号（退号）：费用原路退回，号源释放
     */
    @Transactional
    public String cancel(Integer regId, Integer operatorId) {
        log.info("取消挂号, id={}", regId);
        Registration reg = registrationMapper.selectById(regId);
        if (reg == null) {
            throw new BizException(404, "挂号记录不存在");
        }
        assertOwner(reg.getPatientId(), operatorId, "只能取消本人的挂号");
        if ("已取消".equals(reg.getStatus())) {
            throw new BizException(400, "该挂号已取消");
        }
        if ("已完成".equals(reg.getStatus())) {
            throw new BizException(400, "已完成就诊的挂号不能取消");
        }
        //带前置状态条件：连点两次退号时第二次影响行数为0，不会重复退钱
        if (registrationMapper.cancel(regId) == 0) {
            throw new BizException(400, "该挂号状态已变更，请刷新后重试");
        }

        Payment payment = paymentMapper.selectByBiz("挂号费", regId);
        if (payment != null) {
            //未支付的订单关闭、已支付的退款；两条 SQL 各自带前置状态条件，重复退号不会二次改写
            paymentMapper.cancel(payment.getId());
            paymentMapper.refund(payment.getId());
        }

        //释放号源（加号源本身带了 remain_count<total_count 上限）
        Schedule schedule = scheduleMapper.selectByDoctorDateSlot(reg.getDoctorId(),
                dateKey(reg.getWorkDate()), reg.getTimeSlot());
        if (schedule != null) {
            scheduleMapper.addRemain(schedule.getId());
        }
        return "退号成功，费用已原路退回";
    }

    /**
     * 我的挂号记录
     */
    public List<Registration> myList(Integer patientId, Integer operatorId) {
        assertOwner(patientId, operatorId, "只能查看本人的挂号记录");
        return registrationMapper.selectByPatientId(patientId);
    }

    /** 客户端传来的 id 不可信，必须与登录身份一致 */
    private void assertOwner(Integer ownerId, Integer operatorId, String msg) {
        if (!Objects.equals(ownerId, operatorId)) {
            throw new BizException(403, msg);
        }
    }

    /** registration 表没有 schedule_id，退号只能按 医生+日期+时段 回查排班 */
    private String dateKey(Date workDate) {
        return new SimpleDateFormat("yyyy-MM-dd").format(workDate);
    }
}

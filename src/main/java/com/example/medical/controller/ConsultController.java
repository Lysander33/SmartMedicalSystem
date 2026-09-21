package com.example.medical.controller;

import com.example.medical.auth.LoginContext;
import com.example.medical.auth.LoginUser;
import com.example.medical.auth.Role;
import com.example.medical.entity.Consultation;
import com.example.medical.entity.Doctor;
import com.example.medical.entity.Message;
import com.example.medical.entity.Visitor;
import com.example.medical.mapper.ConsultationMapper;
import com.example.medical.mapper.DoctorMapper;
import com.example.medical.mapper.MessageMapper;
import com.example.medical.mapper.VisitorMapper;
import com.example.medical.util.BizException;
import com.example.medical.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 在线咨询（线上门诊）接口。
 * /api/consult/** 患者端与医生端都要访问，因此归属校验对两种身份分别判定。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/consult")
@Tag(name = "在线咨询（患者端/医生端共用）")
public class ConsultController {

    @Autowired
    private ConsultationMapper consultationMapper;
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private DoctorMapper doctorMapper;
    @Autowired
    private VisitorMapper visitorMapper;

    /**
     * 发起咨询：选择医生与问诊类型，描述病情
     */
    @Operation(summary = "发起咨询")
    @PostMapping("/start")
    public Result<Consultation> start(@RequestBody Consultation consultation) {
        Integer me = LoginContext.currentPatientId();
        log.info("发起咨询, patientId={}, doctorId={}, type={}",
                me, consultation.getDoctorId(), consultation.getType());
        if (consultation.getDescription() == null || consultation.getDescription().isBlank()) {
            throw new BizException(400, "请填写病情描述");
        }
        Doctor doctor = doctorMapper.selectById(consultation.getDoctorId());
        if (doctor == null) {
            throw new BizException(404, "医生不存在");
        }
        if ("离线".equals(doctor.getOnlineStatus())) {
            throw new BizException(400, "该医生当前离线，请选择其他医生");
        }
        Visitor visitor = visitorMapper.selectById(consultation.getVisitorId());
        if (visitor == null) {
            throw new BizException(404, "就诊人不存在");
        }
        if (!me.equals(visitor.getPatientId())) {
            throw new BizException(403, "该就诊人不属于当前登录账号");
        }
        //患者ID与医生姓名一律取服务端数据，不信前端传的值
        consultation.setPatientId(me);
        consultation.setVisitorName(visitor.getName());
        consultation.setDoctorName(doctor.getName());
        consultationMapper.insert(consultation);

        //病情描述作为第一条问诊消息
        Message msg = new Message();
        msg.setConsultationId(consultation.getId());
        msg.setSender("patient");
        msg.setContent("【病情描述】" + consultation.getDescription());
        messageMapper.insert(msg);
        return Result.success(consultation);
    }

    /**
     * 发送问诊消息（患者端图文对话）
     */
    @Operation(summary = "发送问诊消息")
    @PostMapping("/send")
    public Result<String> send(@RequestBody Message message) {
        Integer me = LoginContext.currentPatientId();
        if (message.getConsultationId() == null || message.getContent() == null || message.getContent().isBlank()) {
            throw new BizException(400, "缺少问诊ID或消息内容");
        }
        Consultation c = consultationMapper.selectById(message.getConsultationId());
        if (c == null) {
            throw new BizException(404, "问诊记录不存在");
        }
        if (!me.equals(c.getPatientId())) {
            throw new BizException(403, "该问诊不属于当前登录账号");
        }
        message.setSender("patient");
        messageMapper.insert(message);
        return Result.success("发送成功");
    }

    /**
     * 查看问诊对话记录（患者看自己的问诊，医生看分配给自己的问诊）
     */
    @Operation(summary = "查看问诊对话记录")
    @GetMapping("/messages")
    public Result<List<Message>> messages(@Parameter(description = "问诊ID", required = true, example = "1")
                                          @RequestParam @Positive(message = "问诊ID必须为正数") Integer consultationId) {
        requireVisible(consultationId);
        return Result.successList(messageMapper.selectByConsultationId(consultationId));
    }

    /**
     * 我的问诊记录
     */
    @Operation(summary = "我的问诊记录")
    @GetMapping("/my")
    public Result<List<Consultation>> my(@Parameter(description = "患者ID", required = true, example = "1")
                                         @RequestParam @Positive(message = "患者ID必须为正数") Integer patientId) {
        Integer me = LoginContext.currentPatientId();
        if (!me.equals(patientId)) {
            throw new BizException(403, "只能查看本人的问诊记录");
        }
        return Result.successList(consultationMapper.selectByPatientId(patientId));
    }

    /**
     * 查询问诊详情（患者端与医生端都用，按身份校验归属）
     */
    @Operation(summary = "查询问诊详情")
    @GetMapping("/detail")
    public Result<Consultation> detail(@Parameter(description = "问诊ID", required = true, example = "1")
                                       @RequestParam @Positive(message = "问诊ID必须为正数") Integer id) {
        return Result.success(requireVisible(id));
    }

    /** 问诊归属校验：医生看自己的接诊，患者看自己的问诊 */
    private Consultation requireVisible(Integer consultationId) {
        LoginUser user = LoginContext.require();
        Consultation c = consultationMapper.selectById(consultationId);
        if (c == null) {
            throw new BizException(404, "问诊记录不存在");
        }
        boolean mine = Role.DOCTOR.equals(user.getRole())
                ? user.getRefId().equals(c.getDoctorId())
                : user.getRefId().equals(c.getPatientId());
        if (!mine) {
            throw new BizException(403, "该问诊不属于当前登录账号");
        }
        return c;
    }
}

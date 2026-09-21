package com.example.medical.controller;

import com.example.medical.auth.AuthService;
import com.example.medical.auth.LoginContext;
import com.example.medical.entity.Patient;
import com.example.medical.entity.Visitor;
import com.example.medical.mapper.PatientMapper;
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
import java.util.Map;

/**
 * 患者端-账户与就诊人相关接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/patient")
@Tag(name = "患者端-账户与就诊人")
public class PatientController {

    @Autowired
    private PatientMapper patientMapper;
    @Autowired
    private VisitorMapper visitorMapper;
    @Autowired
    private AuthService authService;

    /**
     * 用户注册：手机号+密码（同事务写入 patient 档案与 sys_user 账号）
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Patient> register(@RequestBody Patient patient) {
        log.info("患者注册, phone={}", patient.getPhone());
        if (patient.getPhone() == null || patient.getPhone().isBlank()
                || patient.getPassword() == null || patient.getPassword().isBlank()) {
            throw new BizException(400, "手机号和密码不能为空");
        }
        if (patient.getName() == null || patient.getName().isBlank()) {
            patient.setName("患者" + patient.getPhone().substring(patient.getPhone().length() - 4));
        }
        Patient saved = authService.registerPatient(patient);
        saved.setPassword(null);
        return Result.success(saved);
    }

    /**
     * 用户登录：与 /api/auth/login 共用同一套逻辑，返回结构也一致（token + profile）
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Patient patient) {
        log.info("患者登录, phone={}", patient.getPhone());
        if (patient.getPhone() == null || patient.getPassword() == null) {
            throw new BizException(400, "手机号和密码不能为空");
        }
        return Result.success(authService.login("patient", patient.getPhone(), patient.getPassword()));
    }

    /**
     * 查询个人信息
     */
    @Operation(summary = "查询个人信息")
    @GetMapping("/info")
    public Result<Patient> info(@Parameter(description = "患者ID", required = true, example = "1")
                                @RequestParam @Positive(message = "患者ID必须为正数") Integer id) {
        Patient patient = patientMapper.selectById(requireMe(id));
        if (patient == null) {
            throw new BizException(404, "患者不存在");
        }
        patient.setPassword(null);
        return Result.success(patient);
    }

    /**
     * 修改个人资料（实名信息绑定）
     */
    @Operation(summary = "修改个人资料")
    @PostMapping("/update")
    public Result<String> update(@RequestBody Patient patient) {
        //资料只能改自己的：ID 以登录身份为准，忽略请求体里的 id
        patient.setId(LoginContext.currentPatientId());
        patientMapper.updateInfo(patient);
        return Result.success("保存成功");
    }

    /**
     * 就诊人列表
     */
    @Operation(summary = "就诊人列表")
    @GetMapping("/visitor/list")
    public Result<List<Visitor>> visitorList(@Parameter(description = "患者ID", required = true, example = "1")
                                             @RequestParam @Positive(message = "患者ID必须为正数") Integer patientId) {
        return Result.successList(visitorMapper.selectByPatientId(requireMe(patientId)));
    }

    /**
     * 添加就诊人（最多5人）
     */
    @Operation(summary = "添加就诊人")
    @PostMapping("/visitor/add")
    public Result<Visitor> addVisitor(@RequestBody Visitor visitor) {
        visitor.setPatientId(LoginContext.currentPatientId());
        if (visitor.getName() == null || visitor.getName().isBlank()) {
            throw new BizException(400, "请填写就诊人姓名");
        }
        if (visitorMapper.countByPatientId(visitor.getPatientId()) >= 5) {
            throw new BizException(400, "每个账号最多添加5名就诊人");
        }
        visitorMapper.insert(visitor);
        return Result.success(visitor);
    }

    /**
     * 修改就诊人
     */
    @Operation(summary = "修改就诊人")
    @PostMapping("/visitor/update")
    public Result<String> updateVisitor(@RequestBody Visitor visitor) {
        if (visitor.getId() == null) {
            throw new BizException(400, "缺少就诊人ID");
        }
        requireMyVisitor(visitor.getId());
        visitor.setPatientId(LoginContext.currentPatientId());
        visitorMapper.update(visitor);
        return Result.success("修改成功");
    }

    /**
     * 删除就诊人
     */
    @Operation(summary = "删除就诊人")
    @GetMapping("/visitor/delete")
    public Result<String> deleteVisitor(@Parameter(description = "就诊人ID", required = true, example = "1")
                                        @RequestParam @Positive(message = "就诊人ID必须为正数") Integer id) {
        requireMyVisitor(id);
        visitorMapper.delete(id);
        return Result.success("删除成功");
    }

    /** 只能操作本人的数据 */
    private Integer requireMe(Integer patientId) {
        Integer me = LoginContext.currentPatientId();
        if (!me.equals(patientId)) {
            throw new BizException(403, "只能操作本人的数据");
        }
        return me;
    }

    /** 校验就诊人归属 */
    private void requireMyVisitor(Integer visitorId) {
        Visitor visitor = visitorMapper.selectById(visitorId);
        if (visitor == null) {
            throw new BizException(404, "就诊人不存在");
        }
        if (!LoginContext.currentPatientId().equals(visitor.getPatientId())) {
            throw new BizException(403, "该就诊人不属于当前登录账号");
        }
    }
}

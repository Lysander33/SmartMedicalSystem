package com.example.medical.controller;

import com.example.medical.auth.LoginContext;
import com.example.medical.auth.LoginUser;
import com.example.medical.entity.QaQuestion;
import com.example.medical.mapper.QaQuestionMapper;
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
 * 患者端-在线问答接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/qa")
@Tag(name = "患者端-在线问答")
public class QaController {

    @Autowired
    private QaQuestionMapper qaQuestionMapper;

    /**
     * 发起咨询问题
     */
    @Operation(summary = "发起咨询问题")
    @PostMapping("/ask")
    public Result<QaQuestion> ask(@RequestBody QaQuestion question) {
        LoginUser me = LoginContext.require();
        log.info("患者提问, patientId={}", me.getRefId());
        if (question.getContent() == null || question.getContent().isBlank()) {
            throw new BizException(400, "问题内容不能为空");
        }
        //患者身份一律取服务端，不信前端传的 patientId
        question.setPatientId(me.getRefId());
        question.setPatientName(me.getName());
        qaQuestionMapper.insert(question);
        return Result.success(question);
    }

    /**
     * 我的问答列表
     */
    @Operation(summary = "我的问答列表")
    @GetMapping("/my")
    public Result<List<QaQuestion>> my(@Parameter(description = "患者ID", required = true, example = "1")
                                       @RequestParam @Positive(message = "患者ID必须为正数") Integer patientId) {
        Integer me = LoginContext.currentPatientId();
        if (!me.equals(patientId)) {
            throw new BizException(403, "只能查看本人的提问");
        }
        return Result.successList(qaQuestionMapper.selectByPatientId(patientId));
    }
}

package com.example.medical.controller;

import com.example.medical.auth.LoginContext;
import com.example.medical.entity.TriageRecord;
import com.example.medical.mapper.TriageRecordMapper;
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
 * AI预分诊接口：根据症状给出建议科室与就诊建议
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/triage")
@Tag(name = "患者端-AI预分诊")
public class TriageController {

    @Autowired
    private TriageRecordMapper triageRecordMapper;

    //分诊规则：关键词 -> 建议科室
    private static final String[][] RULES = {
            {"胸痛|胸闷|心悸|心慌|高血压", "心血管内科"},
            {"呼吸困难|气喘|哮喘|憋气", "呼吸内科"},
            {"咳嗽|发热|发烧|感冒|嗓子|咽痛|流鼻涕", "呼吸内科"},
            {"腹痛|腹泻|胃疼|胃痛|恶心|呕吐|消化不良|拉肚子|便秘", "消化内科"},
            {"头痛|头晕|失眠|麻木|神经衰弱", "神经内科"},
            {"牙痛|牙疼|口腔|龋齿|溃疡", "口腔科"},
            {"皮疹|皮肤瘙痒|瘙痒|湿疹|痤疮|痘痘|荨麻疹", "皮肤科"},
            {"骨折|扭伤|关节|腰疼|腰痛|颈椎|腰椎|腿疼", "骨科"},
            {"儿童|小孩|宝宝|婴儿|孩子", "儿科"},
    };
    //高危症状 -> 急诊提示
    private static final String[] EMERGENCY = {"胸痛", "呼吸困难", "昏迷", "大出血", "高热不退", "意识不清"};

    /**
     * 提交症状信息，输出AI分诊建议
     */
    @Operation(summary = "提交症状信息，输出AI分诊建议")
    @PostMapping("/submit")
    public Result<TriageRecord> submit(@RequestBody TriageRecord record) {
        Integer me = LoginContext.currentPatientId();
        log.info("AI预分诊, patientId={}, symptoms={}", me, record.getSymptoms());
        if (record.getSymptoms() == null || record.getSymptoms().isBlank()) {
            throw new BizException(400, "请填写症状描述");
        }
        //患者身份一律取服务端，不信前端传的 patientId
        record.setPatientId(me);
        String text = (record.getBodyPart() == null ? "" : record.getBodyPart())
                + " " + record.getSymptoms();

        //1.匹配建议科室
        String dept = "全科医学科";
        for (String[] rule : RULES) {
            if (text.matches(".*(" + rule[0] + ").*")) {
                dept = rule[1];
                break;
            }
        }
        //2.判定紧急程度
        String urgency = "普通";
        StringBuilder advice = new StringBuilder();
        for (String kw : EMERGENCY) {
            if (text.contains(kw)) {
                urgency = "急诊";
                advice.append("检测到高危症状【").append(kw).append("】，请尽快前往线下急诊就医！");
                break;
            }
        }
        if (advice.length() == 0) {
            advice.append("建议您挂【").append(dept).append("】普通门诊，就诊时请携带既往病历与检查报告。");
        }
        //分诊结果仅供参考
        advice.append("（本结果由AI预分诊模型给出，仅供参考，不构成诊断）");

        record.setSuggestedDept(dept);
        record.setUrgency(urgency);
        record.setAdvice(advice.toString());
        triageRecordMapper.insert(record);
        return Result.success(record);
    }

    /**
     * 历史预分诊记录
     */
    @Operation(summary = "历史预分诊记录")
    @GetMapping("/history")
    public Result<List<TriageRecord>> history(@Parameter(description = "患者ID", required = true, example = "1")
                                              @RequestParam @Positive(message = "患者ID必须为正数") Integer patientId) {
        Integer me = LoginContext.currentPatientId();
        if (!me.equals(patientId)) {
            throw new BizException(403, "只能查看本人的分诊记录");
        }
        return Result.successList(triageRecordMapper.selectByPatientId(patientId));
    }
}

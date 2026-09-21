package com.example.medical.controller;

import com.example.medical.entity.Department;
import com.example.medical.entity.Doctor;
import com.example.medical.entity.Schedule;
import com.example.medical.mapper.DepartmentMapper;
import com.example.medical.mapper.DoctorMapper;
import com.example.medical.mapper.ScheduleMapper;
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
 * 就诊引导：科室/医生/排班查询接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/guide")
@Tag(name = "患者端-科室医生排班查询")
public class GuideController {

    @Autowired
    private DepartmentMapper departmentMapper;
    @Autowired
    private DoctorMapper doctorMapper;
    @Autowired
    private ScheduleMapper scheduleMapper;

    /**
     * 查询所有科室
     */
    @Operation(summary = "查询所有科室")
    @GetMapping("/depts")
    public Result<List<Department>> depts() {
        return Result.successList(departmentMapper.selectAll());
    }

    /**
     * 按科室查询医生（含医生简介、擅长、评分）
     */
    @Operation(summary = "按科室查询医生")
    @GetMapping("/doctors")
    public Result<List<Doctor>> doctors(@Parameter(description = "科室ID", required = true, example = "1")
                                        @RequestParam @Positive(message = "科室ID必须为正数") Integer deptId) {
        return Result.successList(doctorMapper.selectByDept(deptId));
    }

    /**
     * 查询所有医生（医生端/药房端选择身份用）
     */
    @Operation(summary = "查询所有医生")
    @GetMapping("/doctor/all")
    public Result<List<Doctor>> allDoctors() {
        return Result.successList(doctorMapper.selectAll());
    }

    /**
     * 查询医生详情
     */
    @Operation(summary = "查询医生详情")
    @GetMapping("/doctor")
    public Result<Doctor> doctor(@Parameter(description = "医生ID", required = true, example = "1")
                                 @RequestParam @Positive(message = "医生ID必须为正数") Integer id) {
        Doctor doctor = doctorMapper.selectById(id);
        if (doctor == null) {
            return Result.error(404, "医生不存在");
        }
        return Result.success(doctor);
    }

    /**
     * 查询医生排班与剩余号源
     */
    @Operation(summary = "查询医生排班与剩余号源")
    @GetMapping("/schedules")
    public Result<List<Schedule>> schedules(@Parameter(description = "医生ID", required = true, example = "1")
                                            @RequestParam @Positive(message = "医生ID必须为正数") Integer doctorId) {
        return Result.successList(scheduleMapper.selectByDoctorId(doctorId));
    }
}

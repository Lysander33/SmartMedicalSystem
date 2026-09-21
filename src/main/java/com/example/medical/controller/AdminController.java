package com.example.medical.controller;

import com.example.medical.auth.AuthService;
import com.example.medical.auth.Role;
import com.example.medical.entity.Doctor;
import com.example.medical.entity.Medicine;
import com.example.medical.entity.Patient;
import com.example.medical.entity.Payment;
import com.example.medical.entity.QaQuestion;
import com.example.medical.entity.Schedule;
import com.example.medical.mapper.ConsultationMapper;
import com.example.medical.mapper.DoctorMapper;
import com.example.medical.mapper.MedicineMapper;
import com.example.medical.mapper.PatientMapper;
import com.example.medical.mapper.PaymentMapper;
import com.example.medical.mapper.QaQuestionMapper;
import com.example.medical.mapper.RegistrationMapper;
import com.example.medical.mapper.ScheduleMapper;
import com.example.medical.mapper.SysUserMapper;
import com.example.medical.util.BizException;
import com.example.medical.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理平台-在线问答、医生管理、患者管理、药品信息管理、订单信息管理接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/admin")
@Tag(name = "管理平台-后台管理")
public class AdminController {

    /** 离职状态：软删除，历史数据全部保留 */
    private static final int DOCTOR_RESIGNED = 2;

    @Autowired
    private DoctorMapper doctorMapper;
    @Autowired
    private PatientMapper patientMapper;
    @Autowired
    private MedicineMapper medicineMapper;
    @Autowired
    private PaymentMapper paymentMapper;
    @Autowired
    private QaQuestionMapper qaQuestionMapper;
    @Autowired
    private ScheduleMapper scheduleMapper;
    @Autowired
    private ConsultationMapper consultationMapper;
    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private AuthService authService;

    // ==================== 在线问答管理 ====================

    /**
     * 问答列表（待处理队列+已回复）
     */
    @Operation(summary = "问答列表")
    @GetMapping("/qa/list")
    public Result<List<QaQuestion>> qaList() {
        return Result.successList(qaQuestionMapper.selectAll());
    }

    /**
     * 在线解答患者问题
     */
    @Operation(summary = "在线解答患者问题")
    @PostMapping("/qa/answer")
    public Result<String> answer(@Parameter(description = "问题ID", required = true, example = "1")
                                 @RequestParam @Positive(message = "问题ID必须为正数") Integer id,
                                 @Parameter(description = "解答内容", required = true)
                                 @RequestParam @NotBlank(message = "解答内容不能为空") String answer) {
        log.info("管理员解答问题, id={}", id);
        qaQuestionMapper.answer(id, answer);
        return Result.success("解答成功，已回复给患者");
    }

    // ==================== 医生管理 ====================

    /**
     * 医生列表
     */
    @Operation(summary = "医生列表")
    @GetMapping("/doctor/list")
    public Result<List<Doctor>> doctorList() {
        return Result.successList(doctorMapper.selectAll());
    }

    /**
     * 新增医生：同时开通登录账号（工号+初始密码）
     */
    @Operation(summary = "新增医生")
    @PostMapping("/doctor/add")
    @Transactional
    public Result<Doctor> addDoctor(@RequestBody Doctor doctor) {
        log.info("新增医生, name={}", doctor.getName());
        if (doctor.getName() == null || doctor.getName().isBlank() || doctor.getDeptId() == null) {
            throw new BizException(400, "医生姓名和所属科室不能为空");
        }
        if (doctor.getFee() == null) {
            doctor.setFee(0.0);
        }
        //管理员可以指定登录工号，不填则按医生ID自动生成
        String account = doctor.getAccount() == null ? "" : doctor.getAccount().trim();
        if (!account.isEmpty() && sysUserMapper.selectByUsername(account) != null) {
            throw new BizException(400, "工号【" + account + "】已被使用，请更换");
        }
        String password = (doctor.getPassword() == null || doctor.getPassword().isBlank())
                ? "123456" : doctor.getPassword();

        doctorMapper.insert(doctor);
        if (account.isEmpty()) {
            String base = "D" + (1000 + doctor.getId());
            account = base;
            for (int i = 1; sysUserMapper.selectByUsername(account) != null; i++) {
                account = base + "-" + i;
            }
        }
        authService.createAccount(Role.DOCTOR, doctor.getId(), account, password, doctor.getName());

        doctor.setAccount(account);
        doctor.setPassword(null);
        return Result.success(doctor);
    }

    /**
     * 修改医生信息
     */
    @Operation(summary = "修改医生信息")
    @PostMapping("/doctor/update")
    @Transactional
    public Result<String> updateDoctor(@RequestBody Doctor doctor) {
        if (doctor.getId() == null) {
            throw new BizException(400, "缺少医生ID");
        }
        doctorMapper.update(doctor);
        //改名时同步登录账号的显示名称
        if (doctor.getName() != null && !doctor.getName().isBlank()) {
            authService.syncAccountName(Role.DOCTOR, doctor.getId(), doctor.getName());
        }
        return Result.success("修改成功");
    }

    /**
     * 重置医生密码（医生忘记密码时由管理员重置）
     */
    @Operation(summary = "重置医生密码")
    @PostMapping("/doctor/resetpwd")
    public Result<String> resetDoctorPassword(@Parameter(description = "医生ID", required = true, example = "1")
                                              @RequestParam @Positive(message = "医生ID必须为正数") Integer id,
                                              @Parameter(description = "新密码", required = true, example = "123456")
                                              @RequestParam @NotBlank(message = "新密码不能为空") String newPassword) {
        log.info("管理员重置医生密码, id={}", id);
        if (doctorMapper.selectById(id) == null) {
            throw new BizException(404, "医生不存在");
        }
        sysUserMapper.updatePassword(Role.DOCTOR, id, newPassword);
        return Result.success("密码已重置");
    }

    /**
     * 开通/停用医生账号
     */
    @Operation(summary = "开通/停用医生账号")
    @PostMapping("/doctor/status")
    @Transactional
    public Result<String> doctorStatus(@Parameter(description = "医生ID", required = true, example = "1")
                                       @RequestParam @Positive(message = "医生ID必须为正数") Integer id,
                                       @Parameter(description = "1开通 0停用", required = true, example = "1")
                                       @RequestParam Integer status) {
        doctorMapper.updateStatus(id, status);
        //登录的唯一开关在 sys_user.status，必须同步
        authService.syncAccountStatus(Role.DOCTOR, id, status);
        return Result.success(status == 1 ? "账号已开通" : "账号已停用");
    }

    /**
     * 医生离职（软删除）：停用登录账号并从挂号列表下架，历史处方/挂号/病历全部保留
     */
    @Operation(summary = "医生离职（软删除，高危操作）")
    @PostMapping("/doctor/delete")
    @Transactional
    public Result<String> deleteDoctor(@Parameter(description = "医生ID", required = true, example = "1")
                                       @RequestParam @Positive(message = "医生ID必须为正数") Integer id) {
        log.info("管理员办理医生离职, id={}", id);
        Doctor doctor = doctorMapper.selectById(id);
        if (doctor == null) {
            throw new BizException(404, "医生不存在");
        }
        if (doctor.getStatus() != null && doctor.getStatus() == DOCTOR_RESIGNED) {
            throw new BizException(400, "医生【" + doctor.getName() + "】已处于离职状态");
        }
        //安全校验：还有未完成（待接诊/问诊中）的问诊时禁止离职
        int activeConsult = consultationMapper.countActiveByDoctorId(id);
        if (activeConsult > 0) {
            throw new BizException(400, "该医生还有 " + activeConsult + " 个未完成的问诊（待接诊/问诊中），请先处理完毕再办理离职");
        }
        //安全校验：还有待支付/已支付的挂号时禁止离职
        int activeReg = registrationMapper.countActiveByDoctorId(id);
        if (activeReg > 0) {
            throw new BizException(400, "该医生还有 " + activeReg + " 个未就诊的挂号（待支付/已支付），请先处理完毕再办理离职");
        }
        //软删除：医生置为离职 + 停用登录账号 + 清除其未开始的排班号源
        doctorMapper.updateStatus(id, DOCTOR_RESIGNED);
        authService.syncAccountStatus(Role.DOCTOR, id, 0);
        scheduleMapper.deleteByDoctorId(id);
        return Result.success("医生【" + doctor.getName() + "】（工号" + doctor.getAccount()
                + "）已办理离职：账号停用并从挂号列表下架，历史处方/挂号/病历全部保留可追溯");
    }

    /**
     * 医生在线状态管理
     */
    @Operation(summary = "医生在线状态管理")
    @PostMapping("/doctor/online")
    public Result<String> doctorOnline(@Parameter(description = "医生ID", required = true, example = "1")
                                       @RequestParam @Positive(message = "医生ID必须为正数") Integer id,
                                       @Parameter(description = "在线/离线/忙碌", required = true, example = "在线")
                                       @RequestParam @NotBlank(message = "在线状态不能为空") String onlineStatus) {
        doctorMapper.updateOnlineStatus(id, onlineStatus);
        return Result.success("在线状态已更新为：" + onlineStatus);
    }

    /**
     * 排班管理：新增排班号源
     */
    @Operation(summary = "新增排班号源")
    @PostMapping("/schedule/add")
    public Result<Schedule> addSchedule(@RequestBody Schedule schedule) {
        log.info("新增排班, doctorId={}, workDate={}", schedule.getDoctorId(), schedule.getWorkDate());
        if (schedule.getDoctorId() == null || schedule.getWorkDate() == null || schedule.getTimeSlot() == null) {
            throw new BizException(400, "医生、出诊日期、时段不能为空");
        }
        if (schedule.getTotalCount() == null || schedule.getTotalCount() <= 0) {
            throw new BizException(400, "号源数量必须为正数");
        }
        if (scheduleMapper.selectByDoctorDateSlot(schedule.getDoctorId(),
                new java.text.SimpleDateFormat("yyyy-MM-dd").format(schedule.getWorkDate()),
                schedule.getTimeSlot()) != null) {
            throw new BizException(400, "该医生当天的该时段已有排班，请勿重复添加");
        }
        scheduleMapper.insert(schedule);
        return Result.success(schedule);
    }

    /**
     * 排班查询
     */
    @Operation(summary = "医生排班查询")
    @GetMapping("/schedule/list")
    public Result<List<Schedule>> scheduleList(@Parameter(description = "医生ID", required = true, example = "1")
                                               @RequestParam @Positive(message = "医生ID必须为正数") Integer doctorId) {
        return Result.successList(scheduleMapper.selectByDoctorId(doctorId));
    }

    // ==================== 患者管理 ====================

    /**
     * 患者列表
     */
    @Operation(summary = "患者列表")
    @GetMapping("/patient/list")
    public Result<List<Patient>> patientList() {
        return Result.successList(patientMapper.selectAll());
    }

    /**
     * 停用/恢复患者账号
     */
    @Operation(summary = "停用/恢复患者账号")
    @PostMapping("/patient/status")
    @Transactional
    public Result<String> patientStatus(@Parameter(description = "患者ID", required = true, example = "1")
                                        @RequestParam @Positive(message = "患者ID必须为正数") Integer id,
                                        @Parameter(description = "1恢复 0停用", required = true, example = "1")
                                        @RequestParam Integer status) {
        patientMapper.updateStatus(id, status);
        authService.syncAccountStatus(Role.PATIENT, id, status);
        return Result.success(status == 1 ? "账号已恢复" : "账号已停用");
    }

    // ==================== 药品信息管理 ====================

    /**
     * 药品列表
     */
    @Operation(summary = "药品列表")
    @GetMapping("/medicine/list")
    public Result<List<Medicine>> medicineList() {
        return Result.successList(medicineMapper.selectAll());
    }

    /**
     * 新增药品
     */
    @Operation(summary = "新增药品")
    @PostMapping("/medicine/add")
    public Result<Medicine> addMedicine(@RequestBody Medicine medicine) {
        log.info("新增药品, name={}", medicine.getName());
        if (medicine.getName() == null || medicine.getName().isBlank() || medicine.getPrice() == null) {
            throw new BizException(400, "药品名称和价格不能为空");
        }
        if (medicine.getStock() == null) {
            medicine.setStock(0);
        }
        if (medicine.getWarnStock() == null) {
            medicine.setWarnStock(0);
        }
        medicineMapper.insert(medicine);
        return Result.success(medicine);
    }

    /**
     * 修改药品
     */
    @Operation(summary = "修改药品")
    @PostMapping("/medicine/update")
    public Result<String> updateMedicine(@RequestBody Medicine medicine) {
        if (medicine.getId() == null) {
            throw new BizException(400, "缺少药品ID");
        }
        medicineMapper.update(medicine);
        return Result.success("修改成功");
    }

    /**
     * 药品入库（库存增加）
     */
    @Operation(summary = "药品入库")
    @PostMapping("/medicine/stock")
    public Result<String> addStock(@Parameter(description = "药品ID", required = true, example = "1")
                                   @RequestParam @Positive(message = "药品ID必须为正数") Integer id,
                                   @Parameter(description = "入库数量", required = true, example = "100")
                                   @RequestParam @Positive(message = "入库数量必须为正数") Integer quantity) {
        log.info("药品入库, id={}, quantity={}", id, quantity);
        medicineMapper.addStock(id, quantity);
        return Result.success("入库成功");
    }

    /**
     * 药品上下架
     */
    @Operation(summary = "药品上下架")
    @PostMapping("/medicine/status")
    public Result<String> medicineStatus(@Parameter(description = "药品ID", required = true, example = "1")
                                         @RequestParam @Positive(message = "药品ID必须为正数") Integer id,
                                         @Parameter(description = "上架/下架", required = true, example = "上架")
                                         @RequestParam @NotBlank(message = "状态不能为空") String status) {
        medicineMapper.updateStatus(id, status);
        return Result.success("药品已" + status);
    }

    // ==================== 订单信息管理 ====================

    /**
     * 订单查询（挂号、缴费、处方订单统一查询）
     */
    @Operation(summary = "订单统一查询")
    @GetMapping("/order/list")
    public Result<List<Payment>> orderList() {
        return Result.successList(paymentMapper.selectAll());
    }

    /**
     * 统计报表：挂号量、缴费金额、处方量、发药完成情况
     */
    @Operation(summary = "运营统计报表")
    @GetMapping("/order/stats")
    public Result<Map<String, Object>> stats() {
        List<Payment> orders = paymentMapper.selectAll();
        Map<String, Object> map = new HashMap<>();
        long regCount = orders.stream().filter(o -> "挂号费".equals(o.getBizType())).count();
        long rxCount = orders.stream().filter(o -> "药品费".equals(o.getBizType())).count();
        double paidAmount = orders.stream().filter(o -> "已支付".equals(o.getStatus()))
                .mapToDouble(Payment::getAmount).sum();
        long pendingCount = orders.stream().filter(o -> "待支付".equals(o.getStatus())).count();
        long refundCount = orders.stream().filter(o -> "已退款".equals(o.getStatus())).count();
        map.put("挂号订单量", regCount);
        map.put("处方订单量", rxCount);
        map.put("已缴费总金额", paidAmount);
        map.put("待支付订单量", pendingCount);
        map.put("退款订单量", refundCount);
        map.put("订单总量", orders.size());
        return Result.success(map);
    }
}

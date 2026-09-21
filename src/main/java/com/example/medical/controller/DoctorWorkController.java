package com.example.medical.controller;

import com.example.medical.auth.LoginContext;
import com.example.medical.entity.Consultation;
import com.example.medical.entity.Doctor;
import com.example.medical.entity.MedicalRecord;
import com.example.medical.entity.Medicine;
import com.example.medical.entity.Message;
import com.example.medical.entity.Payment;
import com.example.medical.entity.Prescription;
import com.example.medical.entity.PrescriptionItem;
import com.example.medical.entity.PrescriptionRequest;
import com.example.medical.entity.Registration;
import com.example.medical.mapper.ConsultationMapper;
import com.example.medical.mapper.DoctorMapper;
import com.example.medical.mapper.MedicalRecordMapper;
import com.example.medical.mapper.MedicineMapper;
import com.example.medical.mapper.MessageMapper;
import com.example.medical.mapper.PaymentMapper;
import com.example.medical.mapper.PrescriptionItemMapper;
import com.example.medical.mapper.PrescriptionMapper;
import com.example.medical.mapper.RegistrationMapper;
import com.example.medical.util.BizException;
import com.example.medical.util.NoGenerator;
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

import java.util.List;

/**
 * 医生端-在线接诊、问诊、开具电子处方、门诊就诊、病历查看接口。
 * 医生身份统一从 LoginContext 取（拦截器已解析令牌），只能操作本人的数据。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/doctorwork")
@Tag(name = "医生端-接诊问诊开方")
public class DoctorWorkController {

    @Autowired
    private ConsultationMapper consultationMapper;
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private PrescriptionMapper prescriptionMapper;
    @Autowired
    private PrescriptionItemMapper prescriptionItemMapper;
    @Autowired
    private MedicineMapper medicineMapper;
    @Autowired
    private PaymentMapper paymentMapper;
    @Autowired
    private MedicalRecordMapper medicalRecordMapper;
    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private DoctorMapper doctorMapper;

    // ==================== 在线问诊 ====================

    /**
     * 接诊提醒：本人的待接诊列表
     */
    @Operation(summary = "待接诊列表")
    @GetMapping("/pending")
    public Result<List<Consultation>> pending() {
        return Result.successList(consultationMapper.selectByDoctorId(LoginContext.currentDoctorId(), "待接诊"));
    }

    /**
     * 我的问诊列表（全部状态）
     */
    @Operation(summary = "我的问诊列表")
    @GetMapping("/consultations")
    public Result<List<Consultation>> consultations() {
        return Result.successList(consultationMapper.selectAllByDoctorId(LoginContext.currentDoctorId()));
    }

    /**
     * 确认接诊
     */
    @Operation(summary = "确认接诊")
    @PostMapping("/accept")
    @Transactional
    public Result<String> accept(@Parameter(description = "问诊ID", required = true, example = "1")
                                 @RequestParam @Positive(message = "问诊ID必须为正数") Integer consultationId) {
        Integer doctorId = LoginContext.currentDoctorId();
        log.info("医生接诊, doctorId={}, consultationId={}", doctorId, consultationId);
        Consultation c = requireMyConsultation(consultationId, doctorId);
        if (!"待接诊".equals(c.getStatus())) {
            throw new BizException(400, "该问诊已接诊或已结束");
        }
        if (consultationMapper.updateStatusFrom(consultationId, "问诊中", "待接诊") == 0) {
            throw new BizException(400, "该问诊已被接诊，请刷新后重试");
        }
        Message msg = new Message();
        msg.setConsultationId(consultationId);
        msg.setSender("doctor");
        msg.setContent("您好，我是" + c.getDoctorName() + "医生，已接诊，请描述您的具体症状。");
        messageMapper.insert(msg);
        return Result.success("接诊成功");
    }

    /**
     * 医生回复消息（图文对话）
     */
    @Operation(summary = "医生回复消息")
    @PostMapping("/send")
    public Result<String> send(@RequestBody Message message) {
        Integer doctorId = LoginContext.currentDoctorId();
        if (message.getConsultationId() == null || message.getContent() == null || message.getContent().isBlank()) {
            throw new BizException(400, "缺少问诊ID或消息内容");
        }
        requireMyConsultation(message.getConsultationId(), doctorId);
        message.setSender("doctor");
        messageMapper.insert(message);
        return Result.success("发送成功");
    }

    /**
     * 查看问诊对话记录
     */
    @Operation(summary = "查看问诊对话记录")
    @GetMapping("/messages")
    public Result<List<Message>> messages(@Parameter(description = "问诊ID", required = true, example = "1")
                                          @RequestParam @Positive(message = "问诊ID必须为正数") Integer consultationId) {
        requireMyConsultation(consultationId, LoginContext.currentDoctorId());
        return Result.successList(messageMapper.selectByConsultationId(consultationId));
    }

    /**
     * 结束问诊：填写问诊小结与诊断，同事务归档问诊病历
     */
    @Operation(summary = "结束问诊（生成问诊小结与病历）")
    @PostMapping("/finish")
    @Transactional
    public Result<String> finish(@Parameter(description = "问诊ID", required = true, example = "1")
                                 @RequestParam @Positive(message = "问诊ID必须为正数") Integer consultationId,
                                 @Parameter(description = "问诊小结（诊疗建议）", required = true)
                                 @RequestParam @NotBlank(message = "问诊小结不能为空") String summary,
                                 @Parameter(description = "诊断结论，留空则取问诊小结前200字")
                                 @RequestParam(required = false) String diagnosis) {
        Integer doctorId = LoginContext.currentDoctorId();
        log.info("结束问诊, doctorId={}, consultationId={}", doctorId, consultationId);
        Consultation c = requireMyConsultation(consultationId, doctorId);
        if ("已结束".equals(c.getStatus())) {
            throw new BizException(400, "该问诊已结束，请勿重复操作");
        }
        //先流转状态，影响行数为0说明已被别处结束
        if (consultationMapper.finish(consultationId, summary) == 0) {
            throw new BizException(400, "该问诊已结束，请勿重复操作");
        }
        //同事务归档问诊病历（consultation_id 唯一键兜住重复归档）
        MedicalRecord record = new MedicalRecord();
        record.setPatientId(c.getPatientId());
        record.setVisitorId(c.getVisitorId());
        record.setVisitorName(c.getVisitorName());
        record.setDoctorId(c.getDoctorId());
        record.setDoctorName(c.getDoctorName());
        record.setDeptName(deptNameOf(doctorId));
        record.setType("问诊");
        record.setConsultationId(c.getId());
        record.setDiagnosis(blankToFallback(diagnosis, summary, 200));
        record.setAdvice(summary);
        medicalRecordMapper.insert(record);
        return Result.success("问诊已结束，问诊小结与病历已生成");
    }

    // ==================== 门诊挂号就诊 ====================

    /**
     * 挂号患者队列：本人已缴费、待接诊的挂号单
     */
    @Operation(summary = "挂号患者队列")
    @GetMapping("/reg/queue")
    public Result<List<Registration>> regQueue() {
        return Result.successList(registrationMapper.selectQueueByDoctorId(LoginContext.currentDoctorId()));
    }

    /**
     * 完成门诊就诊：写挂号单完成时间，同事务归档门诊病历
     */
    @Operation(summary = "完成门诊就诊（归档门诊病历）")
    @PostMapping("/visit/finish")
    @Transactional
    public Result<String> finishVisit(@Parameter(description = "挂号单ID", required = true, example = "1")
                                      @RequestParam @Positive(message = "挂号单ID必须为正数") Integer registrationId,
                                      @Parameter(description = "诊断结论", required = true)
                                      @RequestParam @NotBlank(message = "请填写诊断结论") String diagnosis,
                                      @Parameter(description = "医嘱") @RequestParam(required = false) String advice) {
        Integer doctorId = LoginContext.currentDoctorId();
        log.info("完成就诊, doctorId={}, registrationId={}", doctorId, registrationId);
        Registration reg = registrationMapper.selectById(registrationId);
        if (reg == null) {
            throw new BizException(404, "挂号记录不存在");
        }
        if (!doctorId.equals(reg.getDoctorId())) {
            throw new BizException(403, "该挂号不属于当前登录的医生");
        }
        if (!"已支付".equals(reg.getStatus())) {
            throw new BizException(400, "该挂号当前状态为【" + reg.getStatus() + "】，只有已缴费的挂号可以完成就诊");
        }
        //先流转状态，影响行数为0说明已被别处完成
        if (registrationMapper.finish(registrationId) == 0) {
            throw new BizException(400, "该挂号已就诊完成，请勿重复操作");
        }
        MedicalRecord record = new MedicalRecord();
        record.setPatientId(reg.getPatientId());
        record.setVisitorId(reg.getVisitorId());
        record.setVisitorName(reg.getVisitorName());
        record.setDoctorId(reg.getDoctorId());
        record.setDoctorName(reg.getDoctorName());
        record.setDeptName(reg.getDeptName());
        record.setType("门诊");
        record.setRegistrationId(reg.getId());
        record.setDiagnosis(diagnosis);
        record.setAdvice(advice);
        medicalRecordMapper.insert(record);
        return Result.success("就诊已完成，门诊病历已归档");
    }

    // ==================== 电子处方 ====================

    /**
     * 开具电子处方：选择药品、填写用法用量，校验库存后提交，流转药房
     */
    @Operation(summary = "开具电子处方")
    @PostMapping("/rx/create")
    @Transactional
    public Result<Prescription> createRx(@RequestBody PrescriptionRequest request) {
        Integer doctorId = LoginContext.currentDoctorId();
        log.info("开具处方, doctorId={}, consultationId={}", doctorId, request.getConsultationId());
        if (request.getConsultationId() == null) {
            throw new BizException(400, "缺少问诊ID");
        }
        if (request.getDiagnosis() == null || request.getDiagnosis().isBlank()) {
            throw new BizException(400, "请填写诊断结论");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BizException(400, "处方中至少选择一种药品");
        }
        Consultation c = requireMyConsultation(request.getConsultationId(), doctorId);

        //处方校验：药品状态与库存校验（此处只读预检，真正扣库存在药房发药时做）
        double total = 0;
        for (PrescriptionItem item : request.getItems()) {
            Medicine m = medicineMapper.selectById(item.getMedicineId());
            if (m == null) {
                throw new BizException(404, "药品不存在，id=" + item.getMedicineId());
            }
            if (!"上架".equals(m.getStatus())) {
                throw new BizException(400, "药品【" + m.getName() + "】已下架，请更换药品");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BizException(400, "药品【" + m.getName() + "】数量必须为正数");
            }
            if (m.getStock() < item.getQuantity()) {
                throw new BizException(400, "药品【" + m.getName() + "】库存不足（当前库存" + m.getStock() + "），请更换药品或减少数量");
            }
            item.setMedicineName(m.getName());
            item.setSpec(m.getSpec());
            item.setPrice(m.getPrice());
            item.setSubtotal(m.getPrice() * item.getQuantity());
            total += item.getSubtotal();
        }

        Prescription rx = new Prescription();
        rx.setRxNo(NoGenerator.rxNo());
        rx.setConsultationId(c.getId());
        rx.setPatientId(c.getPatientId());
        rx.setVisitorName(c.getVisitorName());
        rx.setDoctorId(c.getDoctorId());
        rx.setDoctorName(c.getDoctorName());
        rx.setDiagnosis(request.getDiagnosis());
        rx.setTotalAmount(total);
        prescriptionMapper.insert(rx);

        for (PrescriptionItem item : request.getItems()) {
            item.setPrescriptionId(rx.getId());
            prescriptionItemMapper.insert(item);
        }

        //生成药品费待缴订单
        Payment payment = new Payment();
        payment.setOrderNo(NoGenerator.orderNo());
        payment.setPatientId(c.getPatientId());
        payment.setBizType("药品费");
        payment.setBizId(rx.getId());
        payment.setTitle("药品费-处方" + rx.getRxNo());
        payment.setAmount(total);
        paymentMapper.insert(payment);

        rx.setItems(request.getItems());
        return Result.success(rx);
    }

    /**
     * 处方作废（未缴费前可作废）
     */
    @Operation(summary = "处方作废")
    @PostMapping("/rx/cancel")
    @Transactional
    public Result<String> cancelRx(@Parameter(description = "处方ID", required = true, example = "1")
                                   @RequestParam @Positive(message = "处方ID必须为正数") Integer rxId) {
        Integer doctorId = LoginContext.currentDoctorId();
        log.info("作废处方, doctorId={}, rxId={}", doctorId, rxId);
        Prescription rx = prescriptionMapper.selectById(rxId);
        if (rx == null) {
            throw new BizException(404, "处方不存在");
        }
        if (!doctorId.equals(rx.getDoctorId())) {
            throw new BizException(403, "该处方不是当前登录医生开具的，无权作废");
        }
        if (!"待缴费".equals(rx.getStatus())) {
            throw new BizException(400, "只有待缴费状态的处方可以作废");
        }
        if (prescriptionMapper.updateStatusFrom(rxId, "已作废", "待缴费") == 0) {
            throw new BizException(400, "该处方状态已变更，请刷新后重试");
        }
        Payment payment = paymentMapper.selectByBiz("药品费", rxId);
        if (payment != null) {
            paymentMapper.refund(payment.getId());
        }
        return Result.success("处方已作废，已同步通知药房与患者");
    }

    /**
     * 我的处方历史
     */
    @Operation(summary = "我的处方历史")
    @GetMapping("/rx/my")
    public Result<List<Prescription>> myRx() {
        List<Prescription> list = prescriptionMapper.selectByDoctorId(LoginContext.currentDoctorId());
        for (Prescription p : list) {
            p.setItems(prescriptionItemMapper.selectByPrescriptionId(p.getId()));
        }
        return Result.successList(list);
    }

    /**
     * 药品字典查询（开方选药用，仅上架药品）
     */
    @Operation(summary = "药品字典查询（开方选药）")
    @GetMapping("/medicines")
    public Result<List<Medicine>> medicines() {
        return Result.successList(medicineMapper.selectOnShelf());
    }

    /**
     * 查看就诊人病历与历史记录
     */
    @Operation(summary = "查看患者病历与历史处方")
    @GetMapping("/patient/history")
    public Result<List<MedicalRecord>> patientHistory(@Parameter(description = "就诊人ID", required = true, example = "1")
                                                      @RequestParam @Positive(message = "就诊人ID必须为正数") Integer visitorId) {
        LoginContext.currentDoctorId();
        return Result.successList(medicalRecordMapper.selectByVisitorId(visitorId));
    }

    // ==================== 共用校验 ====================

    /** 取问诊并校验归属，非本人的问诊直接拒绝 */
    private Consultation requireMyConsultation(Integer consultationId, Integer doctorId) {
        Consultation c = consultationMapper.selectById(consultationId);
        if (c == null) {
            throw new BizException(404, "问诊不存在");
        }
        if (!doctorId.equals(c.getDoctorId())) {
            throw new BizException(403, "该问诊不属于当前登录的医生，无权操作");
        }
        return c;
    }

    private String deptNameOf(Integer doctorId) {
        Doctor doctor = doctorMapper.selectById(doctorId);
        return doctor == null ? null : doctor.getDeptName();
    }

    /** 诊断留空时取小结前 max 字兜底（medical_record.diagnosis 非空） */
    private String blankToFallback(String diagnosis, String fallback, int max) {
        if (diagnosis != null && !diagnosis.isBlank()) {
            return diagnosis;
        }
        if (fallback == null) {
            return "";
        }
        return fallback.length() > max ? fallback.substring(0, max) : fallback;
    }
}

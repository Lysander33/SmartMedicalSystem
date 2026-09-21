package com.example.medical.controller;

import com.example.medical.entity.Medicine;
import com.example.medical.entity.Payment;
import com.example.medical.entity.Prescription;
import com.example.medical.entity.PrescriptionItem;
import com.example.medical.mapper.MedicineMapper;
import com.example.medical.mapper.PaymentMapper;
import com.example.medical.mapper.PrescriptionItemMapper;
import com.example.medical.mapper.PrescriptionMapper;
import com.example.medical.util.BizException;
import com.example.medical.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 药房-处方接收、核对、配药发药、完成备注接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/pharmacy")
@Tag(name = "药房-处方发放")
public class PharmacyController {

    @Autowired
    private PrescriptionMapper prescriptionMapper;
    @Autowired
    private PrescriptionItemMapper prescriptionItemMapper;
    @Autowired
    private PaymentMapper paymentMapper;
    @Autowired
    private MedicineMapper medicineMapper;

    /**
     * 处方列表（按状态筛选：待缴费/待发药/已完成/已作废，不传查全部）
     */
    @Operation(summary = "处方列表（按状态筛选）")
    @GetMapping("/rx/list")
    public Result<List<Prescription>> list(@Parameter(description = "处方状态", example = "待发药")
                                           @RequestParam(required = false) String status) {
        List<Prescription> list = (status == null || status.isBlank())
                ? prescriptionMapper.selectAll()
                : prescriptionMapper.selectByStatus(status);
        return Result.successList(list);
    }

    /**
     * 处方详情：核对患者信息、药品明细、用法用量与缴费状态
     */
    @Operation(summary = "处方详情（核对）")
    @GetMapping("/rx/detail")
    public Result<Prescription> detail(@Parameter(description = "处方ID", required = true, example = "1")
                                       @RequestParam @Positive(message = "处方ID必须为正数") Integer id) {
        Prescription rx = prescriptionMapper.selectById(id);
        if (rx == null) {
            throw new BizException(404, "处方不存在");
        }
        rx.setItems(prescriptionItemMapper.selectByPrescriptionId(id));
        return Result.success(rx);
    }

    /**
     * 发药并备注完成状态：核对缴费状态，扣减库存，状态同步患者端与医生端
     */
    @Operation(summary = "配药发药（完成备注）")
    @PostMapping("/rx/dispense")
    @Transactional
    public Result<String> dispense(@Parameter(description = "处方ID", required = true, example = "1")
                                   @RequestParam @Positive(message = "处方ID必须为正数") Integer rxId,
                                   @Parameter(description = "完成备注", example = "已发药，患者已取走")
                                   @RequestParam(defaultValue = "已完成发药") String remark) {
        log.info("药房发药, rxId={}, remark={}", rxId, remark);
        Prescription rx = prescriptionMapper.selectById(rxId);
        if (rx == null) {
            throw new BizException(404, "处方不存在");
        }
        if ("已作废".equals(rx.getStatus())) {
            throw new BizException(400, "该处方已作废，不能发药");
        }
        if ("已完成".equals(rx.getStatus())) {
            throw new BizException(400, "该处方已完成发药，请勿重复操作");
        }
        if (!"待发药".equals(rx.getStatus())) {
            throw new BizException(400, "该处方当前状态为【" + rx.getStatus() + "】，尚未缴费，暂缓发药");
        }
        //缴费状态核对
        Payment payment = paymentMapper.selectByBiz("药品费", rxId);
        if (payment == null || !"已支付".equals(payment.getStatus())) {
            throw new BizException(400, "该处方药品费未缴纳，暂缓发药，请提示患者先完成缴费");
        }

        List<PrescriptionItem> items = prescriptionItemMapper.selectByPrescriptionId(rxId);
        if (items.isEmpty()) {
            throw new BizException(400, "该处方没有药品明细，无法发药");
        }
        //第一步：全量预检库存（只读）。旧写法边循环边扣，扣到一半失败时前面已扣的库存不会回滚。
        for (PrescriptionItem item : items) {
            Medicine m = medicineMapper.selectById(item.getMedicineId());
            if (m == null) {
                throw new BizException(400, "药品【" + item.getMedicineName() + "】已不在药品库中，请通知医生重新开方");
            }
            if (m.getStock() < item.getQuantity()) {
                throw new BizException(400, "药品【" + item.getMedicineName() + "】库存不足（需 "
                        + item.getQuantity() + "，现有 " + m.getStock() + "），已标注缺药待补，请通知医生更换药品");
            }
        }
        //第二步：扣减库存。影响行数为0说明并发下被别人抢走了，抛异常让整个发药回滚
        for (PrescriptionItem item : items) {
            if (medicineMapper.reduceStock(item.getMedicineId(), item.getQuantity()) == 0) {
                throw new BizException(400, "药品【" + item.getMedicineName() + "】库存不足，本次发药已回滚，请通知医生更换药品");
            }
        }
        //第三步：置为已完成（带前置状态条件，防重复发药）
        if (prescriptionMapper.finish(rxId, "已完成", remark) == 0) {
            throw new BizException(400, "该处方已发药或状态已变更，请刷新后重试");
        }
        return Result.success("发药完成，状态已同步至患者端与医生端");
    }
}

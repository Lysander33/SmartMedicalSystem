package com.example.medical.mapper;

import com.example.medical.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 操作缴费订单表的接口
 */
@Mapper
public interface PaymentMapper {
    int insert(Payment payment);
    Payment selectById(Integer id);
    List<Payment> selectByPatientId(Integer patientId);
    List<Payment> selectPendingByPatientId(Integer patientId);
    Payment selectByBiz(@Param("bizType") String bizType, @Param("bizId") Integer bizId);
    List<Payment> selectAll();
    int pay(@Param("id") Integer id, @Param("payType") String payType, @Param("ticketNo") String ticketNo);
    /** 未支付订单的关闭：仅"待支付"可改为"已取消" */
    int cancel(Integer id);
    /** 已支付订单的退款：仅"已支付"可改为"已退款" */
    int refund(Integer id);
}

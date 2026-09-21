package com.example.medical.mapper;

import com.example.medical.entity.Registration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 操作挂号记录表的接口。
 * 所有状态流转都带前置状态条件（where id=? and status=?），调用方必须检查影响行数，
 * 否则"连点两次支付"这类重复操作会二次改写状态。
 */
@Mapper
public interface RegistrationMapper {
    int insert(Registration registration);
    List<Registration> selectByPatientId(Integer patientId);
    Registration selectById(Integer id);
    /** 医生端：本人全部挂号记录 */
    List<Registration> selectByDoctorId(Integer doctorId);
    /** 医生端：本人已缴费、待接诊的挂号队列 */
    List<Registration> selectQueueByDoctorId(Integer doctorId);
    /** 支付挂号费：仅"待支付"可改为"已支付" */
    int pay(@Param("id") Integer id, @Param("payType") String payType);
    /** 退号：仅"待支付/已支付"可改为"已取消" */
    int cancel(Integer id);
    /** 完成就诊：仅"已支付"可改为"已完成"并写入就诊完成时间 */
    int finish(Integer id);
    int countConflict(@Param("visitorId") Integer visitorId, @Param("scheduleId") Integer scheduleId);
    int countActiveByDoctorId(Integer doctorId);
}

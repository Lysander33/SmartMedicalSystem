package com.example.medical.mapper;

import com.example.medical.entity.Consultation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 操作在线咨询表的接口
 */
@Mapper
public interface ConsultationMapper {
    int insert(Consultation consultation);
    List<Consultation> selectByPatientId(Integer patientId);
    List<Consultation> selectByDoctorId(@Param("doctorId") Integer doctorId, @Param("status") String status);
    List<Consultation> selectAllByDoctorId(Integer doctorId);
    Consultation selectById(Integer id);
    /** 状态流转，带前置状态条件；影响行数为 0 说明状态已被别的请求改过 */
    int updateStatusFrom(@Param("id") Integer id, @Param("status") String status,
                         @Param("fromStatus") String fromStatus);
    /** 结束问诊：仅"待接诊/问诊中"可改为"已结束" */
    int finish(@Param("id") Integer id, @Param("summary") String summary);
    int countActiveByDoctorId(Integer doctorId);
}

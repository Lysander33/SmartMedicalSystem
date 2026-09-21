package com.example.medical.mapper;

import com.example.medical.entity.Prescription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 操作电子处方表的接口
 */
@Mapper
public interface PrescriptionMapper {
    int insert(Prescription prescription);
    Prescription selectById(Integer id);
    List<Prescription> selectByPatientId(Integer patientId);
    List<Prescription> selectByDoctorId(Integer doctorId);
    List<Prescription> selectByStatus(String status);
    List<Prescription> selectAll();
    /** 状态流转，带前置状态条件；影响行数为 0 说明状态已被别的请求改过 */
    int updateStatusFrom(@Param("id") Integer id, @Param("status") String status,
                         @Param("fromStatus") String fromStatus);
    int finish(@Param("id") Integer id, @Param("status") String status, @Param("remark") String remark);
}

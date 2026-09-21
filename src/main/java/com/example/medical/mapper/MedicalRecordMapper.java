package com.example.medical.mapper;

import com.example.medical.entity.MedicalRecord;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 操作病历表的接口
 */
@Mapper
public interface MedicalRecordMapper {
    int insert(MedicalRecord record);
    List<MedicalRecord> selectByPatientId(Integer patientId);
    List<MedicalRecord> selectByVisitorId(Integer visitorId);
}

package com.example.medical.mapper;

import com.example.medical.entity.TriageRecord;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 操作预分诊记录表的接口
 */
@Mapper
public interface TriageRecordMapper {
    int insert(TriageRecord record);
    List<TriageRecord> selectByPatientId(Integer patientId);
}

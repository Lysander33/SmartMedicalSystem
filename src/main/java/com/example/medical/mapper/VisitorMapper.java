package com.example.medical.mapper;

import com.example.medical.entity.Visitor;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 操作就诊人表的接口
 */
@Mapper
public interface VisitorMapper {
    int insert(Visitor visitor);
    List<Visitor> selectByPatientId(Integer patientId);
    Visitor selectById(Integer id);
    int update(Visitor visitor);
    int delete(Integer id);
    int countByPatientId(Integer patientId);
}

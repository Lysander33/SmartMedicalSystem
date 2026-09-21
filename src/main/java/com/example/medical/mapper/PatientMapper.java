package com.example.medical.mapper;

import com.example.medical.entity.Patient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 操作患者账号表的接口
 */
@Mapper
public interface PatientMapper {
    int insert(Patient patient);
    Patient selectByPhone(String phone);
    Patient selectById(Integer id);
    List<Patient> selectAll();
    int updateInfo(Patient patient);
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);
}

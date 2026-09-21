package com.example.medical.mapper;

import com.example.medical.entity.Doctor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 操作医生表的接口。
 * 登录工号/密码已迁到 sys_user，这里的 account 是从 sys_user 关联查出来的只读展示字段。
 */
@Mapper
public interface DoctorMapper {
    List<Doctor> selectAll();
    List<Doctor> selectByDept(Integer deptId);
    Doctor selectById(Integer id);
    int insert(Doctor doctor);
    int update(Doctor doctor);
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);
    int updateOnlineStatus(@Param("id") Integer id, @Param("onlineStatus") String onlineStatus);
}

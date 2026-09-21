package com.example.medical.mapper;

import com.example.medical.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 操作排班号源表的接口
 */
@Mapper
public interface ScheduleMapper {
    List<Schedule> selectByDoctorId(Integer doctorId);
    Schedule selectById(Integer id);
    Schedule selectByDoctorDateSlot(@Param("doctorId") Integer doctorId, @Param("workDate") String workDate, @Param("timeSlot") String timeSlot);
    int reduceRemain(Integer id);
    int addRemain(Integer id);
    int insert(Schedule schedule);
    int delete(Integer id);
    int deleteByDoctorId(Integer doctorId);
}

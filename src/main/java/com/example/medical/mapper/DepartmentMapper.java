package com.example.medical.mapper;

import com.example.medical.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 操作科室表的接口
 */
@Mapper
public interface DepartmentMapper {
    List<Department> selectAll();
    Department selectById(Integer id);
}

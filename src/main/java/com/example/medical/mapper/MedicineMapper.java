package com.example.medical.mapper;

import com.example.medical.entity.Medicine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 操作药品信息表的接口
 */
@Mapper
public interface MedicineMapper {
    List<Medicine> selectAll();
    List<Medicine> selectOnShelf();
    Medicine selectById(Integer id);
    int insert(Medicine medicine);
    int update(Medicine medicine);
    int updateStatus(@Param("id") Integer id, @Param("status") String status);
    int reduceStock(@Param("id") Integer id, @Param("quantity") Integer quantity);
    int addStock(@Param("id") Integer id, @Param("quantity") Integer quantity);
}

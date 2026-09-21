package com.example.medical.mapper;

import com.example.medical.entity.PrescriptionItem;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 操作处方明细表的接口
 */
@Mapper
public interface PrescriptionItemMapper {
    int insert(PrescriptionItem item);
    List<PrescriptionItem> selectByPrescriptionId(Integer prescriptionId);
}

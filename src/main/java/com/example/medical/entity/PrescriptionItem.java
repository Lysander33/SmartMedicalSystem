package com.example.medical.entity;

import lombok.Data;

/**
 * 处方明细（药品条目）
 */
@Data
public class PrescriptionItem {
    private Integer id;
    private Integer prescriptionId;
    private Integer medicineId;
    private String medicineName;
    private String spec;
    private Integer quantity;
    private String dosage;
    private String frequency;
    private String course;
    private Double price;
    private Double subtotal;
}

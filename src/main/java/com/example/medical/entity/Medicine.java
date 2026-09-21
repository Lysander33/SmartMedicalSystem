package com.example.medical.entity;

import lombok.Data;

/**
 * 药品信息
 */
@Data
public class Medicine {
    private Integer id;
    private String name;
    private String genericName;
    private String spec;
    private String category;
    private String manufacturer;
    private Double price;
    private Integer stock;
    private Integer warnStock;
    private String insuranceType;
    private String status;
}

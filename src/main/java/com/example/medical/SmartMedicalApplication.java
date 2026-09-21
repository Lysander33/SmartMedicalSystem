package com.example.medical;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 智慧医疗就诊系统 主类
 */
@SpringBootApplication
//扫描mapper包下所有操作数据库的接口
@MapperScan("com.example.medical.mapper")
public class SmartMedicalApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartMedicalApplication.class, args);
    }

}

package com.example.medical.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 业务单号生成器：前缀 + 秒级时间戳 + 4位自增序号。
 * 旧的 System.currentTimeMillis() 写法在高并发下会撞号（单号列有唯一键，撞号直接报错）。
 */
public final class NoGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    private NoGenerator() {
    }

    public static String next(String prefix) {
        return prefix + LocalDateTime.now().format(FMT) + String.format("%04d", SEQ.incrementAndGet() % 10000);
    }

    /** 挂号单号 */
    public static String regNo() {
        return next("REG");
    }

    /** 缴费订单号 */
    public static String orderNo() {
        return next("PAY");
    }

    /** 处方号 */
    public static String rxNo() {
        return next("RX");
    }

    /** 电子票据号 */
    public static String ticketNo() {
        return next("TICKET");
    }
}

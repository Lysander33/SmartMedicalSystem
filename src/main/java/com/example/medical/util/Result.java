package com.example.medical.util;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 表示的是结果集
 * @param <T>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应结果集")
public class Result<T> {

    @Schema(description = "状态码：200成功", example = "200")
    private Integer code ;
    @Schema(description = "提示信息", example = "success")
    private String msg ;
    @Schema(description = "业务数据")
    private T data ;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<List<T>> successList(List<T> list) {
        return new Result<>(200, "success", list);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

}

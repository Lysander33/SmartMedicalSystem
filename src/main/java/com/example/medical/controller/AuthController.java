package com.example.medical.controller;

import com.example.medical.auth.AuthService;
import com.example.medical.auth.LoginContext;
import com.example.medical.auth.LoginUser;
import com.example.medical.auth.TokenStore;
import com.example.medical.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统一登录接口：选择身份类型（患者/医生/药房/管理员），登录到不同的端
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
@Tag(name = "统一登录入口")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private TokenStore tokenStore;

    /**
     * 统一登录：根据身份类型校验账号密码，返回令牌与对应端的档案信息
     *
     * @param role     身份类型：patient患者 / doctor医生 / pharmacy药房 / admin管理员
     * @param account  账号（患者=手机号，医生=工号如D1001，药房/管理员=员工账号）
     */
    @Operation(summary = "统一登录（按身份类型进入不同的端）")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @Parameter(description = "身份类型：patient/doctor/pharmacy/admin", required = true, example = "patient")
            @RequestParam @NotBlank(message = "请选择身份类型") String role,
            @Parameter(description = "账号", required = true, example = "13800000001")
            @RequestParam @NotBlank(message = "账号不能为空") String account,
            @Parameter(description = "密码", required = true, example = "123456")
            @RequestParam @NotBlank(message = "密码不能为空") String password) {
        return Result.success(authService.login(role, account, password));
    }

    /**
     * 校验当前令牌是否还有效，并返回当前身份。
     * 前端启动时用它核对一次，避免拿 localStorage 里的隔夜废票冒充已登录。
     * 该路径不在 WebMvcConfig 的白名单里，拦截器会自动要求有效令牌。
     */
    @Operation(summary = "校验当前令牌并返回身份")
    @GetMapping("/me")
    public Result<LoginUser> me() {
        return Result.success(LoginContext.require());
    }

    /**
     * 退出登录：服务端作废令牌（旧代码只清了浏览器 localStorage，令牌在服务端仍然有效）
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = "X-Token", required = false) String token) {
        tokenStore.remove(token);
        return Result.success("已退出登录");
    }
}

package com.example.medical.config;

import com.example.medical.auth.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册登录拦截器：/api/** 全部需要登录，只有登录、注册、退出三个入口除外。
 * 全站都是登录后才进得去（页面在 <head> 里就把未登录访客送回 /login.html），
 * 所以科室、医生、排班这些接口也不必再对外公开。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        // 退出登录：令牌可能已失效，这里放行，按"已退出"处理即可
                        "/api/auth/logout",
                        "/api/patient/register");
    }
}

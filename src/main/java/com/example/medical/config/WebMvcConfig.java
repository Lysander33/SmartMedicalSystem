package com.example.medical.config;

import com.example.medical.auth.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册登录拦截器：/api/** 全部需要登录，下列入口除外。
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
                        "/api/patient/login",
                        "/api/patient/register",
                        // 就诊引导必须公开：首页未登录时就要能浏览科室、医生、排班
                        "/api/guide/**");
    }
}

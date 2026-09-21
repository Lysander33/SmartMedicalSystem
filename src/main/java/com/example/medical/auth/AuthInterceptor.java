package com.example.medical.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 登录与角色校验拦截器。
 * 前端把登录签发的 token 放在 X-Token 请求头里，这里解析出身份后放进 LoginContext，
 * 业务代码通过 LoginContext.currentPatientId() / currentDoctorId() 拿身份，
 * 不再靠 URL 上的 ?patientId= / ?token= 参数（那些参数是可伪造的）。
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /**
     * URL 前缀 -> 允许访问的角色。
     * 未命中映射表的 /api/** 一律要求"已登录"，不默认放行（默认拒绝更安全）。
     */
    private static final Map<String, List<String>> RULES = Map.ofEntries(
            Map.entry("/api/admin/", List.of(Role.ADMIN)),
            Map.entry("/api/pharmacy/", List.of(Role.PHARMACY)),
            Map.entry("/api/doctorwork/", List.of(Role.DOCTOR)),
            // 问诊对话患者端和医生端都要看，两边都放行，归属校验在 Controller 里做
            Map.entry("/api/consult/", List.of(Role.PATIENT, Role.DOCTOR)),
            Map.entry("/api/reg/", List.of(Role.PATIENT)),
            Map.entry("/api/pay/", List.of(Role.PATIENT)),
            Map.entry("/api/patient/", List.of(Role.PATIENT)),
            Map.entry("/api/records/", List.of(Role.PATIENT)),
            Map.entry("/api/qa/", List.of(Role.PATIENT)),
            Map.entry("/api/triage/", List.of(Role.PATIENT))
    );

    @Autowired
    private TokenStore tokenStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        //入口先清一次：Tomcat 线程复用时不带上一个请求的身份
        LoginContext.clear();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        LoginUser user = tokenStore.get(request.getHeader("X-Token"));
        if (user == null) {
            writeJson(response, 401, "未登录或登录已失效，请重新登录");
            return false;
        }

        List<String> allowed = matchRules(request.getRequestURI());
        if (allowed != null && !allowed.contains(user.getRole())) {
            log.warn("越权访问被拦截, uri={}, role={}", request.getRequestURI(), user.getRole());
            writeJson(response, 403, "当前【" + Role.label(user.getRole()) + "】身份无权访问该接口");
            return false;
        }

        LoginContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginContext.clear();
    }

    private List<String> matchRules(String uri) {
        for (Map.Entry<String, List<String>> rule : RULES.entrySet()) {
            if (uri.startsWith(rule.getKey())) {
                return rule.getValue();
            }
        }
        return null;
    }

    /**
     * 直接写 JSON 字面量，不引 ObjectMapper
     * （Spring Boot 4 换成了 Jackson 3，注入哪个 ObjectMapper 容易产生歧义）
     */
    private void writeJson(HttpServletResponse response, int code, String msg) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}");
    }
}

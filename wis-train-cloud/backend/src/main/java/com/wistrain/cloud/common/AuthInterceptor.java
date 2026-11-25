package com.wistrain.cloud.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证鉴权拦截器（NF-06）：
 * 1. /api/** 全部校验 JWT（/api/auth/login 除外）
 * 2. /api/admin/** 二次校验角色 = 部门管理员
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            return true;
        }
        String path = req.getRequestURI();
        if (path.equals("/api/auth/login")) {
            return true;
        }

        String token = req.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return reject(resp, 401, "未登录或令牌无效");
        }
        Claims claims;
        try {
            claims = jwtUtil.parse(token.substring(7));
        } catch (Exception e) {
            return reject(resp, 401, "登录已过期，请重新登录");
        }

        UserContext.set(new UserContext.UserInfo(
                Long.valueOf(claims.getSubject()),
                claims.get("role", Integer.class),
                claims.get("deptId", Long.class),
                claims.get("name", String.class)));

        // 管理接口角色校验：USER_ROLE 1=部门管理员 2=员工
        if (path.startsWith("/api/admin") && UserContext.get().role() != 1) {
            UserContext.clear();
            return reject(resp, 403, "无管理员权限");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean reject(HttpServletResponse resp, int code, String msg) throws Exception {
        resp.setStatus(code);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(code, msg)));
        return false;
    }
}

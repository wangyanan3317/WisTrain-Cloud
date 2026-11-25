package com.wistrain.cloud.controller;

import com.wistrain.cloud.common.ApiResult;
import com.wistrain.cloud.dto.LoginReq;
import com.wistrain.cloud.dto.LoginResp;
import com.wistrain.cloud.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录（US-101）：返回 JWT 与角色 */
    @PostMapping("/login")
    public ApiResult<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return ApiResult.ok(authService.login(req));
    }

    /** 退出（US-102）：JWT 无状态，前端清除令牌即可 */
    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        return ApiResult.ok();
    }
}

package com.wistrain.cloud.service;

import com.wistrain.cloud.common.BizException;
import com.wistrain.cloud.common.JwtUtil;
import com.wistrain.cloud.dto.LoginReq;
import com.wistrain.cloud.dto.LoginResp;
import com.wistrain.cloud.entity.User;
import com.wistrain.cloud.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务（M1）：BCrypt 校验 + JWT 签发
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public BCryptPasswordEncoder encoder() {
        return encoder;
    }

    public LoginResp login(LoginReq req) {
        User user = userMapper.findByLogname(req.getLogname());
        // US-101：不泄露账号是否存在
        if (user == null || !encoder.matches(req.getPwd(), user.getPwd())) {
            throw new BizException("账号或密码错误");
        }
        if (user.getState() == 0) {
            throw new BizException("账号已被禁用");
        }
        String token = jwtUtil.create(user.getUserId(), user.getRole(), user.getDeptId(), user.getRealname());
        return LoginResp.builder()
                .token(token)
                .userId(user.getUserId())
                .realname(user.getRealname())
                .role(user.getRole())
                .deptId(user.getDeptId())
                .build();
    }
}

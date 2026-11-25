package com.wistrain.cloud.dto;

import lombok.Builder;
import lombok.Data;

/** 登录响应：JWT + 角色（US-101 角色分流由前端根据 role 路由） */
@Data
@Builder
public class LoginResp {
    private String token;
    private Long userId;
    private String realname;
    private Integer role;
    private Long deptId;
}

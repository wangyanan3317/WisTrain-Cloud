package com.wistrain.cloud.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录请求 */
@Data
public class LoginReq {
    @NotBlank(message = "请输入登录名")
    private String logname;
    @NotBlank(message = "请输入密码")
    private String pwd;
}

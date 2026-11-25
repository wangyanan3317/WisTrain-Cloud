package com.wistrain.cloud.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 用户表 tb_user：1 部门管理员 / 2 企业员工 */
@Data
public class User {
    private Long userId;
    private String logname;
    private String pwd;
    private String realname;
    private String email;
    private Integer role;
    private Integer state;
    private Long deptId;
    private LocalDateTime createTime;
}

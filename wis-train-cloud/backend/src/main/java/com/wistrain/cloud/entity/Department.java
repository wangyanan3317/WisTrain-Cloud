package com.wistrain.cloud.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 部门表 tb_department */
@Data
public class Department {
    private Long deptId;
    private String deptName;
    private String deptCode;
    private Integer deptState;
    private LocalDateTime createTime;
}

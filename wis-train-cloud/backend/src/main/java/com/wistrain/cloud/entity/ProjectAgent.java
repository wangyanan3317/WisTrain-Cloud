package com.wistrain.cloud.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 项目—智能体权限关联表 tb_project_agent：权限联动规则配置 */
@Data
public class ProjectAgent {
    private Long id;
    private Long projectId;
    private Long agentId;
    private Integer unlockRule;       // 1 完成全部资料 / 2 达到进度阈值
    private Integer unlockThreshold;  // 规则 2 时必填
    private LocalDateTime createTime;
}

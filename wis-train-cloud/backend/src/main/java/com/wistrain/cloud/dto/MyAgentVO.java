package com.wistrain.cloud.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 员工「我的智能体」视图（US-404）：含实时解锁状态与所属项目进度 */
@Data
public class MyAgentVO {
    private Long agentId;
    private String agentName;
    private Integer platform;
    private String platformName;    // COZE / DIFY / OPENAI / OTHER
    private String agentDesc;
    private boolean enabled;        // 智能体启用状态
    private boolean unlocked;       // 实时判定结果
    private Long projectId;
    private String projectName;
    private Integer unlockRule;
    private Integer unlockThreshold;
    private Integer progress;       // 关联项目的学习进度
    private LocalDateTime unlockTime;
}

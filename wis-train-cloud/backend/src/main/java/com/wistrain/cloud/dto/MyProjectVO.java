package com.wistrain.cloud.dto;

import lombok.Data;
import java.time.LocalDateTime;

/** 员工「我的项目」视图（US-301）：项目信息 + 进度 + 解锁状态 */
@Data
public class MyProjectVO {
    private Long projectId;
    private String projectName;
    private String projectDesc;
    private Integer projectState;   // 0/1/2
    private LocalDateTime projectStart;
    private LocalDateTime projectEnd;
    private Integer progress;
    private Integer learnState;
    private LocalDateTime unlockTime;
    private long materialTotal;
    private long materialLearned;
    private long agentTotal;
    private long agentUnlocked;
}

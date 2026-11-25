package com.wistrain.cloud.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 项目成员表 tb_project_member：学习进度与解锁冗余（判定以实时计算为准） */
@Data
public class ProjectMember {
    private Long memberId;
    private Long projectId;
    private Long userId;
    private LocalDateTime joinTime;
    private Integer learnState;   // 0 未开始 / 1 学习中 / 2 已完成
    private Integer progress;     // 0-100
    private LocalDateTime unlockTime;
}

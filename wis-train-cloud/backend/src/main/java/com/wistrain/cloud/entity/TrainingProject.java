package com.wistrain.cloud.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 培训项目表 tb_training_project：0 草稿 / 1 进行中 / 2 已结束 */
@Data
public class TrainingProject {
    private Long projectId;
    private String projectName;
    private String projectDesc;
    private Integer projectState;
    private LocalDateTime projectStart;
    private LocalDateTime projectEnd;
    private Long deptId;
    private Long createUserId;
    private LocalDateTime createTime;
}

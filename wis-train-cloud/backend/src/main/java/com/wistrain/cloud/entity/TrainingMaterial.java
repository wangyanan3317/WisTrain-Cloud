package com.wistrain.cloud.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 培训资料表 tb_training_material：1 文档 / 2 视频 / 3 外链 */
@Data
public class TrainingMaterial {
    private Long materialId;
    private Long projectId;
    private String materialName;
    private Integer materialType;
    private String materialUrl;
    private Integer materialOrder;
    private LocalDateTime createTime;
}

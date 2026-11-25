package com.wistrain.cloud.dto;

import lombok.Data;
import java.time.LocalDateTime;

/** 学习中心资料视图（US-303）：资料信息 + 学习状态 */
@Data
public class MaterialVO {
    private Long materialId;
    private String materialName;
    private Integer materialType;   // 1 文档 / 2 视频 / 3 外链
    private String materialUrl;
    private Integer materialOrder;
    private boolean learned;
    private LocalDateTime learnTime;
}

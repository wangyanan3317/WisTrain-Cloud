package com.wistrain.cloud.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 学习记录表 tb_learn_record（设计补充表）：
 * 需求「按资料标记已学」需要逐条记录，原数据字典只有 PROGRESS 汇总字段，
 * 本表补齐「已学资料数 / 总资料数」的分母与分子。
 */
@Data
public class LearnRecord {
    private Long recordId;
    private Long projectId;
    private Long userId;
    private Long materialId;
    private LocalDateTime learnTime;
}

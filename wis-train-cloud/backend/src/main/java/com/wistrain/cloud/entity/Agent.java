package com.wistrain.cloud.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 智能体表 tb_agent：1 Coze / 2 Dify / 3 OpenAI 兼容 / 4 其他 */
@Data
public class Agent {
    private Long agentId;
    private String agentName;
    private Integer agentPlatform;
    private String agentApiKey;   // AES 加密存储，接口不回显明文
    private String agentEndpoint;
    private String agentModel;
    private String agentDesc;
    private Integer agentState;   // 1 启用 / 0 停用
    private Long deptId;
    private Long createUserId;
    private LocalDateTime createTime;
}

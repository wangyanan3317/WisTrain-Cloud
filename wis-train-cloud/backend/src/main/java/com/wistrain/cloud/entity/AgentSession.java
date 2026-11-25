package com.wistrain.cloud.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 智能体会话表 tb_agent_session */
@Data
public class AgentSession {
    private Long sessionId;
    private Long agentId;
    private Long userId;
    private String sessionTitle;
    private LocalDateTime createTime;
    private LocalDateTime lastTime;
}

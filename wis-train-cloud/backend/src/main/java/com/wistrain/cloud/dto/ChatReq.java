package com.wistrain.cloud.dto;

import lombok.Data;

/** 智能体对话请求（US-405）：sessionId 为空则创建新会话 */
@Data
public class ChatReq {
    private Long sessionId;
    private String message;
}

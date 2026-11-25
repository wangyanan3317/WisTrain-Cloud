package com.wistrain.cloud.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** 智能体对话请求（US-405）：sessionId 为空则创建新会话；history 为多轮上下文（最近若干条） */
@Data
public class ChatReq {
    private Long sessionId;
    private String message;
    /** 多轮上下文：[{role: "user"|"assistant", content: "..."}]，不含本轮消息 */
    private List<Map<String, String>> history;
}

package com.wistrain.cloud.controller;

import com.wistrain.cloud.common.ApiResult;
import com.wistrain.cloud.common.UserContext;
import com.wistrain.cloud.dto.ChatReq;
import com.wistrain.cloud.dto.MyAgentVO;
import com.wistrain.cloud.entity.AgentSession;
import com.wistrain.cloud.service.AgentChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 员工智能体接口（M4）：解锁列表 / SSE 会话
 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentChatController {

    private final AgentChatService agentChatService;

    /** 我的智能体（US-404）：含实时解锁状态 */
    @GetMapping("/my")
    public ApiResult<List<MyAgentVO>> my() {
        return ApiResult.ok(agentChatService.myAgents(UserContext.get().userId()));
    }

    /** 会话历史（US-405） */
    @GetMapping("/{id}/sessions")
    public ApiResult<List<AgentSession>> sessions(@PathVariable Long id) {
        return ApiResult.ok(agentChatService.sessions(UserContext.get().userId(), id));
    }

    /**
     * 对话（US-405）：SSE 流式返回。
     * 事件流：message(增量文本) / error(失败) / done(结束, data=sessionId)
     * 说明：SSE 由前端 fetch + ReadableStream 消费（EventSource 不支持 POST/自定义头）。
     */
    @PostMapping(value = "/{id}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@PathVariable Long id, @RequestBody ChatReq req) {
        return agentChatService.chat(UserContext.get().userId(), id, req.getSessionId(), req.getMessage());
    }
}

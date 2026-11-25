package com.wistrain.cloud.controller;

import com.wistrain.cloud.common.ApiResult;
import com.wistrain.cloud.common.UserContext;
import com.wistrain.cloud.dto.PageResult;
import com.wistrain.cloud.entity.Agent;
import com.wistrain.cloud.service.AgentAdminService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端：智能体纳管与权限关联配置（M4）：/api/admin/agents
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAgentController {

    private final AgentAdminService agentAdminService;

    @GetMapping("/agents")
    public ApiResult<PageResult<Agent>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(agentAdminService.list(UserContext.get().deptId(), keyword, pageNum, pageSize));
    }

    /** 录入智能体（US-401）：Key 加密落库，响应不回传明文 */
    @PostMapping("/agents")
    public ApiResult<Agent> create(@RequestBody Agent agent) {
        UserContext.UserInfo u = UserContext.get();
        return ApiResult.ok(agentAdminService.create(u.userId(), u.deptId(), agent));
    }

    /** 编辑（US-402）：Key 传掩码则不更新 */
    @PutMapping("/agents")
    public ApiResult<Void> update(@RequestBody Agent agent) {
        agentAdminService.update(UserContext.get().deptId(), agent);
        return ApiResult.ok();
    }

    /** 启用 / 停用（US-402）：停用后全部关联会话入口关闭 */
    @PutMapping("/agents/{id}/state")
    public ApiResult<Void> updateState(@PathVariable Long id, @RequestBody StateReq req) {
        agentAdminService.updateState(UserContext.get().deptId(), id, req.getState());
        return ApiResult.ok();
    }

    /** 项目的权限关联配置（US-403）：全量替换保存 */
    @PutMapping("/projects/{id}/agents")
    public ApiResult<Void> configure(@PathVariable Long id, @RequestBody List<AgentAdminService.RuleItem> items) {
        agentAdminService.configure(UserContext.get().deptId(), id, items);
        return ApiResult.ok();
    }

    /** 项目的关联配置列表 */
    @GetMapping("/projects/{id}/agents")
    public ApiResult<List<Map<String, Object>>> listConfig(@PathVariable Long id) {
        return ApiResult.ok(agentAdminService.listConfig(UserContext.get().deptId(), id));
    }

    @Data
    public static class StateReq {
        private Integer state;
    }
}

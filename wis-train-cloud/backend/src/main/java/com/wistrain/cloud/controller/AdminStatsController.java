package com.wistrain.cloud.controller;

import com.wistrain.cloud.common.ApiResult;
import com.wistrain.cloud.common.UserContext;
import com.wistrain.cloud.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端：数据看板（US-503）：/api/admin/stats
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;

    @GetMapping("/summary")
    public ApiResult<Map<String, Object>> summary() {
        return ApiResult.ok(statsService.summary(UserContext.get().deptId()));
    }

    /** 近 7 日趋势：joins 加入人数 / chats 会话数 */
    @GetMapping("/trend")
    public ApiResult<List<Map<String, Object>>> trend() {
        return ApiResult.ok(statsService.trend(UserContext.get().deptId()));
    }
}

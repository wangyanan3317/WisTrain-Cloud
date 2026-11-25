package com.wistrain.cloud.controller;

import com.wistrain.cloud.common.ApiResult;
import com.wistrain.cloud.common.UserContext;
import com.wistrain.cloud.dto.PageResult;
import com.wistrain.cloud.entity.User;
import com.wistrain.cloud.service.UserAdminService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端：员工管理（M5）：/api/admin/users
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserAdminService userAdminService;

    @GetMapping
    public ApiResult<PageResult<User>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(userAdminService.page(UserContext.get().deptId(), keyword, pageNum, pageSize));
    }

    /** 新增员工（US-501）：初始密码随机生成并明文返回一次 */
    @PostMapping
    public ApiResult<CreateResp> create(@RequestBody User u) {
        User created = userAdminService.create(UserContext.get().deptId(), u);
        return ApiResult.ok(new CreateResp(created.getUserId(), created.getLogname()));
    }

    /** 禁用 / 启用（US-501） */
    @PutMapping("/{id}/state")
    public ApiResult<Void> updateState(@PathVariable Long id, @RequestBody StateReq req) {
        userAdminService.updateState(UserContext.get().deptId(), id, req.getState());
        return ApiResult.ok();
    }

    /** 重置密码（US-104）：返回临时密码 */
    @PutMapping("/{id}/password")
    public ApiResult<String> resetPwd(@PathVariable Long id) {
        return ApiResult.ok(userAdminService.resetPwd(UserContext.get().deptId(), id));
    }

    @Data
    public static class StateReq {
        private Integer state;
    }

    public record CreateResp(Long userId, String logname) {}
}

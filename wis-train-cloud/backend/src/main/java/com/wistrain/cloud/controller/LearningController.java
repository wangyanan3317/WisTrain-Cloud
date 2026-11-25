package com.wistrain.cloud.controller;

import com.wistrain.cloud.common.ApiResult;
import com.wistrain.cloud.common.UserContext;
import com.wistrain.cloud.dto.MaterialVO;
import com.wistrain.cloud.dto.MyProjectVO;
import com.wistrain.cloud.entity.TrainingProject;
import com.wistrain.cloud.service.LearningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 员工学习接口（M3）：/api/projects
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;

    /** 我的项目（US-301） */
    @GetMapping("/my")
    public ApiResult<List<MyProjectVO>> my() {
        return ApiResult.ok(learningService.myProjects(UserContext.get().userId()));
    }

    /** 可加入项目（US-302） */
    @GetMapping("/available")
    public ApiResult<List<TrainingProject>> available() {
        UserContext.UserInfo u = UserContext.get();
        return ApiResult.ok(learningService.available(u.userId(), u.deptId()));
    }

    /** 加入项目（US-302） */
    @PostMapping("/{id}/join")
    public ApiResult<Void> join(@PathVariable Long id) {
        learningService.join(UserContext.get().userId(), id);
        return ApiResult.ok();
    }

    /** 学习中心资料列表（US-303） */
    @GetMapping("/{id}/materials")
    public ApiResult<List<MaterialVO>> materials(@PathVariable Long id) {
        return ApiResult.ok(learningService.materials(UserContext.get().userId(), id));
    }

    /** 标记资料已学：触发进度重算 + 权限联动（US-303 / REQ-08） */
    @PostMapping("/materials/{materialId}/finish")
    public ApiResult<MyProjectVO> finish(@PathVariable Long materialId) {
        return ApiResult.ok(learningService.finish(UserContext.get().userId(), materialId));
    }
}

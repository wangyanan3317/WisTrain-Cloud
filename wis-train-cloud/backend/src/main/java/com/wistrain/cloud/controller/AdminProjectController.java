package com.wistrain.cloud.controller;

import com.wistrain.cloud.common.ApiResult;
import com.wistrain.cloud.common.UserContext;
import com.wistrain.cloud.dto.PageResult;
import com.wistrain.cloud.entity.TrainingMaterial;
import com.wistrain.cloud.entity.TrainingProject;
import com.wistrain.cloud.mapper.MemberMapper;
import com.wistrain.cloud.service.LearningService;
import com.wistrain.cloud.service.ProjectAdminService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端：培训项目与资料（M2）：/api/admin/projects，仅本部门
 */
@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
public class AdminProjectController {

    private final ProjectAdminService projectAdminService;
    private final LearningService learningService;
    private final MemberMapper memberMapper;
    private final com.wistrain.cloud.mapper.MaterialMapper materialMapper;

    @GetMapping
    public ApiResult<PageResult<TrainingProject>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer state) {
        return ApiResult.ok(projectAdminService.list(
                UserContext.get().deptId(), keyword, state, pageNum, pageSize));
    }

    @PostMapping
    public ApiResult<TrainingProject> create(@RequestBody TrainingProject p) {
        UserContext.UserInfo u = UserContext.get();
        return ApiResult.ok(projectAdminService.create(u.userId(), u.deptId(), p));
    }

    @PutMapping
    public ApiResult<Void> update(@RequestBody TrainingProject p) {
        projectAdminService.update(UserContext.get().deptId(), p);
        return ApiResult.ok();
    }

    /** 发布：草稿 → 进行中（US-201） */
    @PutMapping("/{id}/publish")
    public ApiResult<Void> publish(@PathVariable Long id) {
        projectAdminService.publish(UserContext.get().deptId(), id);
        return ApiResult.ok();
    }

    /** 归档（US-202） */
    @PutMapping("/{id}/archive")
    public ApiResult<Void> archive(@PathVariable Long id) {
        projectAdminService.archive(UserContext.get().deptId(), id);
        return ApiResult.ok();
    }

    /** 项目详情统计（US-205） */
    @GetMapping("/{id}/stats")
    public ApiResult<Map<String, Object>> stats(@PathVariable Long id) {
        return ApiResult.ok(projectAdminService.detailStats(id));
    }

    // ---------- 资料管理（US-203） ----------

    /** 资料列表（管理端详情页） */
    @GetMapping("/{id}/materials")
    public ApiResult<List<TrainingMaterial>> materials(@PathVariable Long id) {
        projectAdminService.requireOwnMaterialView(UserContext.get().deptId(), id);
        return ApiResult.ok(materialMapper.listByProject(id));
    }

    @PostMapping("/{id}/materials")
    public ApiResult<TrainingMaterial> addMaterial(@PathVariable Long id, @RequestBody TrainingMaterial m) {
        m.setProjectId(id);
        return ApiResult.ok(projectAdminService.addMaterial(UserContext.get().deptId(), m));
    }

    @PutMapping("/materials")
    public ApiResult<Void> updateMaterial(@RequestBody TrainingMaterial m) {
        projectAdminService.updateMaterial(UserContext.get().deptId(), m);
        return ApiResult.ok();
    }

    @DeleteMapping("/materials/{materialId}")
    public ApiResult<Void> deleteMaterial(@PathVariable Long materialId) {
        projectAdminService.deleteMaterial(UserContext.get().deptId(), materialId);
        return ApiResult.ok();
    }

    /** 调整学习顺序：body = [materialId...] 按新顺序排列 */
    @PutMapping("/materials/order")
    public ApiResult<Void> reorder(@RequestBody List<Long> materialIds) {
        projectAdminService.reorder(UserContext.get().deptId(), materialIds);
        return ApiResult.ok();
    }

    // ---------- 成员管理（US-204） ----------

    @GetMapping("/{id}/members")
    public ApiResult<List<Map<String, Object>>> members(@PathVariable Long id) {
        return ApiResult.ok(memberMapper.listByProject(id));
    }

    /** 批量分配员工：已加入的自动跳过；其它失败原因汇总回传（如项目未发布） */
    @PostMapping("/{id}/members")
    public ApiResult<Void> assign(@PathVariable Long id, @RequestBody AssignReq req) {
        List<String> failed = new java.util.ArrayList<>();
        List<String> skipped = new java.util.ArrayList<>();
        for (Long userId : req.getUserIds()) {
            try {
                learningService.join(userId, id);
            } catch (com.wistrain.cloud.common.BizException e) {
                if (e.getMessage() != null && e.getMessage().contains("已加入")) {
                    skipped.add("用户#" + userId);
                } else {
                    failed.add("用户#" + userId + "：" + e.getMessage());
                }
            }
        }
        if (!failed.isEmpty()) {
            throw new com.wistrain.cloud.common.BizException(String.join("；", failed));
        }
        return ApiResult.ok();
    }

    @DeleteMapping("/members/{memberId}")
    public ApiResult<Void> removeMember(@PathVariable Long memberId) {
        memberMapper.deleteById(memberId);
        return ApiResult.ok();
    }

    /** 批量分配请求体 */
    @Data
    public static class AssignReq {
        private List<Long> userIds;
    }
}

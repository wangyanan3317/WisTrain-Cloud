package com.wistrain.cloud.service;

import com.wistrain.cloud.common.BizException;
import com.wistrain.cloud.entity.*;
import com.wistrain.cloud.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管理端：培训项目与资料（M2 / M5）
 * 全部操作以 deptId 过滤，保证「仅本部门」（NF-06）
 */
@Service
@RequiredArgsConstructor
public class ProjectAdminService {

    private final ProjectMapper projectMapper;
    private final MaterialMapper materialMapper;
    private final MemberMapper memberMapper;
    private final LearnRecordMapper learnRecordMapper;
    private final LearningService learningService;

    public Page list(Long deptId, String keyword, Integer state, long pageNum, long pageSize) {
        long offset = (pageNum - 1) * pageSize;
        return new Page(projectMapper.countByDept(deptId, keyword, state),
                projectMapper.pageByDept(deptId, keyword, state, offset, pageSize));
    }

    public TrainingProject create(Long userId, Long deptId, TrainingProject p) {
        p.setDeptId(deptId);
        p.setCreateUserId(userId);
        p.setProjectState(0); // 草稿
        if (p.getProjectEnd() != null && p.getProjectStart() != null && p.getProjectEnd().isBefore(p.getProjectStart())) {
            throw new BizException("结束时间不能早于开始时间");
        }
        projectMapper.insert(p);
        return p;
    }

    public void update(Long deptId, TrainingProject p) {
        requireOwn(deptId, p.getProjectId());
        projectMapper.update(p);
    }

    /** 发布（US-202）：草稿 → 进行中 */
    public void publish(Long deptId, Long projectId) {
        TrainingProject p = requireOwn(deptId, projectId);
        if (p.getProjectState() != 0) {
            throw new BizException("仅草稿状态可发布");
        }
        if (materialMapper.countByProject(projectId) == 0) {
            throw new BizException("请先上传培训资料");
        }
        p.setProjectState(1);
        projectMapper.update(p);
    }

    /** 归档：进行中/草稿 → 已结束；已有成员时不可物理删除（US-202） */
    public void archive(Long deptId, Long projectId) {
        TrainingProject p = requireOwn(deptId, projectId);
        p.setProjectState(2);
        projectMapper.update(p);
    }

    public void delete(Long deptId, Long projectId) {
        TrainingProject p = requireOwn(deptId, projectId);
        if (memberMapper.countByDept(p.getDeptId()) > 0 && memberMapper.listByProject(projectId).size() > 0) {
            throw new BizException("已有成员或学习记录的项目不允许删除，请归档");
        }
        projectMapper.update(p); // 保留记录，实际按归档处理
    }

    /** 项目详情统计（US-205） */
    public java.util.Map<String, Object> detailStats(Long projectId) {
        List<java.util.Map<String, Object>> members = memberMapper.listByProject(projectId);
        double avg = members.stream().mapToInt(m -> ((Number) m.get("progress")).intValue()).average().orElse(0);
        long completed = members.stream().filter(m -> ((Number) m.get("learnState")).intValue() == 2).count();
        return java.util.Map.of(
                "memberCount", members.size(),
                "materialCount", materialMapper.countByProject(projectId),
                "avgProgress", Math.round(avg),
                "completedCount", completed);
    }

    /** 资料列表归属校验（供 Controller 使用） */
    public void requireOwnMaterialView(Long deptId, Long projectId) {
        requireOwn(deptId, projectId);
    }

    // ---------- 资料管理（US-203） ----------

    public TrainingMaterial addMaterial(Long deptId, TrainingMaterial m) {
        requireOwn(deptId, m.getProjectId());
        long count = materialMapper.countByProject(m.getProjectId());
        if (m.getMaterialOrder() == null) {
            m.setMaterialOrder((int) count);
        }
        materialMapper.insert(m);
        recomputeAllMembers(m.getProjectId());
        return m;
    }

    public void updateMaterial(Long deptId, TrainingMaterial m) {
        TrainingMaterial old = materialMapper.findById(m.getMaterialId());
        if (old == null) {
            throw new BizException("资料不存在");
        }
        requireOwn(deptId, old.getProjectId());
        materialMapper.update(m);
    }

    /** 删除资料 → 学习记录作废 → 全部成员进度重算（US-203） */
    @Transactional
    public void deleteMaterial(Long deptId, Long materialId) {
        TrainingMaterial m = materialMapper.findById(materialId);
        if (m == null) {
            return;
        }
        requireOwn(deptId, m.getProjectId());
        materialMapper.deleteById(materialId);
        learnRecordMapper.deleteByMaterial(materialId);
        recomputeAllMembers(m.getProjectId());
    }

    /** 调整学习顺序 */
    public void reorder(Long deptId, List<Long> materialIds) {
        for (int i = 0; i < materialIds.size(); i++) {
            TrainingMaterial m = materialMapper.findById(materialIds.get(i));
            if (m != null) {
                requireOwn(deptId, m.getProjectId());
                materialMapper.updateOrder(materialIds.get(i), i);
            }
        }
    }

    private void recomputeAllMembers(Long projectId) {
        for (java.util.Map<String, Object> row : memberMapper.listByProject(projectId)) {
            Long userId = ((Number) row.get("userId")).longValue();
            ProjectMember member = memberMapper.findByProjectAndUser(projectId, userId);
            if (member != null) {
                learningService.recompute(member, projectId, userId);
            }
        }
    }

    private TrainingProject requireOwn(Long deptId, Long projectId) {
        TrainingProject p = projectMapper.findById(projectId);
        if (p == null || !p.getDeptId().equals(deptId)) {
            throw new BizException(403, "项目不存在或无权限");
        }
        return p;
    }

    public record Page(long total, List<TrainingProject> items) {}
}

package com.wistrain.cloud.service;

import com.wistrain.cloud.common.BizException;
import com.wistrain.cloud.dto.MaterialVO;
import com.wistrain.cloud.dto.MyProjectVO;
import com.wistrain.cloud.entity.*;
import com.wistrain.cloud.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 员工学习服务（M3）：我的项目 / 加入 / 学习资料 / 进度
 */
@Service
@RequiredArgsConstructor
public class LearningService {

    private final MemberMapper memberMapper;
    private final ProjectMapper projectMapper;
    private final MaterialMapper materialMapper;
    private final LearnRecordMapper learnRecordMapper;
    private final ProjectAgentMapper projectAgentMapper;
    private final PermissionService permissionService;

    /** 我的项目（US-301）：成员记录 JOIN 项目，聚合资料与智能体计数 */
    public List<MyProjectVO> myProjects(Long userId) {
        List<MyProjectVO> result = new ArrayList<>();
        for (Map<String, Object> row : memberMapper.listMy(userId)) {
            Long projectId = ((Number) row.get("projectId")).longValue();
            MyProjectVO vo = new MyProjectVO();
            vo.setProjectId(projectId);
            vo.setProjectName((String) row.get("projectName"));
            vo.setProjectDesc((String) row.get("projectDesc"));
            vo.setProjectState((Integer) row.get("projectState"));
            vo.setProjectStart((LocalDateTime) row.get("projectStart"));
            vo.setProjectEnd((LocalDateTime) row.get("projectEnd"));
            vo.setProgress((Integer) row.get("progress"));
            vo.setLearnState((Integer) row.get("learnState"));
            vo.setUnlockTime((LocalDateTime) row.get("unlockTime"));
            vo.setMaterialTotal(materialMapper.countByProject(projectId));
            vo.setMaterialLearned(learnRecordMapper.countLearned(projectId, userId));
            long total = projectAgentMapper.listByProject(projectId).size();
            vo.setAgentTotal(total);
            vo.setAgentUnlocked(countUnlocked(userId, projectId, vo.getProgress() == null ? 0 : vo.getProgress()));
            result.add(vo);
        }
        return result;
    }

    private long countUnlocked(Long userId, Long projectId, int progress) {
        return projectAgentMapper.listByProject(projectId).stream()
                .filter(r -> permissionService.satisfied(r, progress))
                .count();
    }

    /** 可加入项目（US-302） */
    public List<TrainingProject> available(Long userId, Long deptId) {
        return projectMapper.listAvailable(userId, deptId);
    }

    /** 加入项目（US-302）：项目须进行中，一人一条记录 */
    public void join(Long userId, Long projectId) {
        TrainingProject p = projectMapper.findById(projectId);
        if (p == null || p.getProjectState() != 1) {
            throw new BizException("项目未发布或已结束");
        }
        if (memberMapper.findByProjectAndUser(projectId, userId) != null) {
            throw new BizException("您已加入该项目");
        }
        ProjectMember m = new ProjectMember();
        m.setProjectId(projectId);
        m.setUserId(userId);
        memberMapper.insert(m);
    }

    /** 学习中心资料列表（US-303）：资料 + 本人学习状态 */
    @Transactional
    public List<MaterialVO> materials(Long userId, Long projectId) {
        requireMember(userId, projectId);
        Map<Long, LocalDateTime> learned = new java.util.HashMap<>();
        for (LearnRecord r : learnRecordMapper.listByProjectAndUser(projectId, userId)) {
            learned.put(r.getMaterialId(), r.getLearnTime());
        }
        List<MaterialVO> result = new ArrayList<>();
        for (TrainingMaterial m : materialMapper.listByProject(projectId)) {
            MaterialVO vo = new MaterialVO();
            vo.setMaterialId(m.getMaterialId());
            vo.setMaterialName(m.getMaterialName());
            vo.setMaterialType(m.getMaterialType());
            vo.setMaterialUrl(m.getMaterialUrl());
            vo.setMaterialOrder(m.getMaterialOrder());
            LocalDateTime t = learned.get(m.getMaterialId());
            vo.setLearned(t != null);
            vo.setLearnTime(t);
            result.add(vo);
        }
        return result;
    }

    /** 标记资料已学（US-303）→ 重算进度 → 权限联动刷新 */
    @Transactional
    public MyProjectVO finish(Long userId, Long materialId) {
        TrainingMaterial material = materialMapper.findById(materialId);
        if (material == null) {
            throw new BizException("资料不存在");
        }
        ProjectMember member = requireMember(userId, material.getProjectId());

        if (learnRecordMapper.exists(material.getProjectId(), userId, materialId) == 0) {
            LearnRecord r = new LearnRecord();
            r.setProjectId(material.getProjectId());
            r.setUserId(userId);
            r.setMaterialId(materialId);
            learnRecordMapper.insert(r);
        }
        recompute(member, material.getProjectId(), userId);
        permissionService.refreshUnlock(userId, material.getProjectId());

        // 返回刷新后的项目视图
        return myProjects(userId).stream()
                .filter(v -> v.getProjectId().equals(material.getProjectId()))
                .findFirst().orElse(null);
    }

    /** 进度 = 已学 / 总数 × 100；资料增删后也走这里重算 */
    public void recompute(ProjectMember member, Long projectId, Long userId) {
        long total = materialMapper.countByProject(projectId);
        long learned = learnRecordMapper.countLearned(projectId, userId);
        int progress = total == 0 ? 0 : (int) Math.round(learned * 100.0 / total);
        member.setProgress(progress);
        member.setLearnState(progress >= 100 ? 2 : (learned > 0 ? 1 : 0));
        memberMapper.update(member);
    }

    private ProjectMember requireMember(Long userId, Long projectId) {
        ProjectMember member = memberMapper.findByProjectAndUser(projectId, userId);
        if (member == null) {
            throw new BizException(403, "您尚未加入该项目");
        }
        return member;
    }
}

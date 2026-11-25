package com.wistrain.cloud.service;

import com.wistrain.cloud.entity.Agent;
import com.wistrain.cloud.entity.ProjectAgent;
import com.wistrain.cloud.entity.ProjectMember;
import com.wistrain.cloud.entity.TrainingProject;
import com.wistrain.cloud.mapper.AgentMapper;
import com.wistrain.cloud.mapper.MemberMapper;
import com.wistrain.cloud.mapper.ProjectAgentMapper;
import com.wistrain.cloud.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 核心机制：权限联动（设计说明书第 3 章）
 *
 * 两条链路复用同一判定逻辑：
 *  1. refreshUnlock —— 学习进度变更后点亮 UNLOCK_TIME（展示冗余）
 *  2. checkAccess   —— 智能体访问前实时判定（NF-08：不信任前端状态）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final ProjectAgentMapper projectAgentMapper;
    private final MemberMapper memberMapper;
    private final ProjectMapper projectMapper;
    private final AgentMapper agentMapper;

    /** 解锁规则判定：1 完成全部资料(progress>=100)；2 达到进度阈值 */
    public boolean satisfied(ProjectAgent rule, int progress) {
        if (rule.getUnlockRule() != null && rule.getUnlockRule() == 2) {
            return rule.getUnlockThreshold() != null && progress >= rule.getUnlockThreshold();
        }
        return progress >= 100;
    }

    /**
     * 进度变更后刷新解锁冗余字段：满足任一规则且尚未点亮时，写入 UNLOCK_TIME。
     * 不回退——规则变更时以 checkAccess 实时判定为准。
     */
    public void refreshUnlock(Long userId, Long projectId) {
        ProjectMember member = memberMapper.findByProjectAndUser(projectId, userId);
        if (member == null || member.getUnlockTime() != null) {
            return;
        }
        boolean any = projectAgentMapper.listByProject(projectId).stream()
                .anyMatch(rule -> satisfied(rule, member.getProgress() == null ? 0 : member.getProgress()));
        if (any) {
            member.setUnlockTime(LocalDateTime.now());
            memberMapper.update(member);
            log.info("权限联动：用户 {} 解锁项目 {} 关联智能体", userId, projectId);
        }
    }

    /**
     * 智能体访问资格实时判定（S-04）：
     * 智能体启用 + 存在「进行中」关联项目 + 本人成员记录满足解锁规则。
     */
    public boolean checkAccess(Long userId, Long agentId) {
        Agent agent = agentMapper.findById(agentId);
        if (agent == null || agent.getAgentState() == 0) {
            return false;
        }
        for (ProjectAgent rule : projectAgentMapper.listByAgent(agentId)) {
            TrainingProject project = projectMapper.findById(rule.getProjectId());
            if (project == null || project.getProjectState() != 1) {
                continue; // 项目归档即关闭访问（Q5）
            }
            ProjectMember member = memberMapper.findByProjectAndUser(rule.getProjectId(), userId);
            if (member != null && satisfied(rule, member.getProgress() == null ? 0 : member.getProgress())) {
                return true;
            }
        }
        return false;
    }
}

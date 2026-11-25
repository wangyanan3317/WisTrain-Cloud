package com.wistrain.cloud.service;

import com.wistrain.cloud.common.BizException;
import com.wistrain.cloud.common.CryptoUtil;
import com.wistrain.cloud.dto.PageResult;
import com.wistrain.cloud.entity.Agent;
import com.wistrain.cloud.entity.ProjectAgent;
import com.wistrain.cloud.entity.TrainingProject;
import com.wistrain.cloud.mapper.AgentMapper;
import com.wistrain.cloud.mapper.ProjectAgentMapper;
import com.wistrain.cloud.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端：智能体纳管与「项目—智能体」权限关联配置（M4 管理侧）
 */
@Service
@RequiredArgsConstructor
public class AgentAdminService {

    private final AgentMapper agentMapper;
    private final ProjectAgentMapper projectAgentMapper;
    private final ProjectMapper projectMapper;

    @Value("${wistrain.aes-key}")
    private String aesKey;

    public PageResult<Agent> list(Long deptId, String keyword, long pageNum, long pageSize) {
        long offset = (pageNum - 1) * pageSize;
        return PageResult.of(agentMapper.countByDept(deptId, keyword),
                agentMapper.pageByDept(deptId, keyword, offset, pageSize));
    }

    /** 录入智能体（US-401）：Key AES 加密落库 */
    public Agent create(Long userId, Long deptId, Agent a) {
        if (a.getAgentApiKey() == null || a.getAgentApiKey().isBlank()) {
            throw new BizException("API Key 不能为空");
        }
        a.setDeptId(deptId);
        a.setCreateUserId(userId);
        a.setAgentApiKey(CryptoUtil.encrypt(a.getAgentApiKey(), aesKey));
        a.setAgentState(1);
        agentMapper.insert(a);
        a.setAgentApiKey(null); // 不回传明文
        return a;
    }

    /** 编辑（US-402）：Key 回显为掩码，传掩码则不更新 */
    public void update(Long deptId, Agent a) {
        Agent old = requireOwn(deptId, a.getAgentId());
        if (a.getAgentApiKey() == null || a.getAgentApiKey().contains("****")) {
            a.setAgentApiKey(null);
        } else {
            a.setAgentApiKey(CryptoUtil.encrypt(a.getAgentApiKey(), aesKey));
        }
        a.setDeptId(old.getDeptId());
        agentMapper.update(a);
    }

    public void updateState(Long deptId, Long id, Integer state) {
        requireOwn(deptId, id);
        agentMapper.updateState(id, state);
    }

    /** 配置「项目—智能体」关联（US-403）：全量替换式保存 */
    @Transactional
    public void configure(Long deptId, Long projectId, List<AgentAdminService.RuleItem> items) {
        TrainingProject project = projectMapper.findById(projectId);
        if (project == null || !project.getDeptId().equals(deptId)) {
            throw new BizException(403, "项目不存在或无权限");
        }
        projectAgentMapper.deleteByProject(projectId);
        for (RuleItem item : items) {
            Agent agent = agentMapper.findById(item.agentId());
            if (agent == null || !agent.getDeptId().equals(deptId)) {
                throw new BizException("智能体不存在或无权限: " + item.agentId());
            }
            if (item.unlockRule() == 2 && (item.unlockThreshold() == null || item.unlockThreshold() < 1 || item.unlockThreshold() > 100)) {
                throw new BizException("进度阈值须为 1—100");
            }
            ProjectAgent pa = new ProjectAgent();
            pa.setProjectId(projectId);
            pa.setAgentId(item.agentId());
            pa.setUnlockRule(item.unlockRule());
            pa.setUnlockThreshold(item.unlockThreshold());
            projectAgentMapper.insert(pa);
        }
    }

    /** 关联配置列表（Key 掩码化） */
    public List<Map<String, Object>> listConfig(Long deptId, Long projectId) {
        TrainingProject project = projectMapper.findById(projectId);
        if (project == null || !project.getDeptId().equals(deptId)) {
            throw new BizException(403, "项目不存在或无权限");
        }
        List<Map<String, Object>> rows = projectAgentMapper.listConfigByProject(projectId);
        rows.forEach(r -> r.put("agentKeyMasked", "****")); // 规则配置页不展示 Key
        return rows;
    }

    private Agent requireOwn(Long deptId, Long id) {
        Agent a = agentMapper.findById(id);
        if (a == null || !a.getDeptId().equals(deptId)) {
            throw new BizException(403, "智能体不存在或无权限");
        }
        return a;
    }

    public record RuleItem(Long agentId, Integer unlockRule, Integer unlockThreshold) {}
}

package com.wistrain.cloud.mapper;

import com.wistrain.cloud.entity.ProjectAgent;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProjectAgentMapper {

    @Insert("INSERT INTO tb_project_agent(PROJECT_ID, AGENT_ID, UNLOCK_RULE, UNLOCK_THRESHOLD) " +
            "VALUES(#{projectId}, #{agentId}, #{unlockRule}, #{unlockThreshold})")
    int insert(ProjectAgent pa);

    @Delete("DELETE FROM tb_project_agent WHERE PROJECT_ID = #{projectId}")
    int deleteByProject(Long projectId);

    @Select("SELECT * FROM tb_project_agent WHERE PROJECT_ID = #{projectId}")
    List<ProjectAgent> listByProject(Long projectId);

    @Select("SELECT * FROM tb_project_agent WHERE AGENT_ID = #{agentId}")
    List<ProjectAgent> listByAgent(Long agentId);

    @Select("SELECT * FROM tb_project_agent WHERE PROJECT_ID = #{projectId} AND AGENT_ID = #{agentId}")
    ProjectAgent findByProjectAndAgent(@Param("projectId") Long projectId, @Param("agentId") Long agentId);

    /** 管理端：项目的权限关联配置（含智能体信息，US-403 / 8 章配置页） */
    @Select("SELECT pa.ID AS id, pa.PROJECT_ID AS projectId, pa.AGENT_ID AS agentId, " +
            "pa.UNLOCK_RULE AS unlockRule, pa.UNLOCK_THRESHOLD AS unlockThreshold, pa.CREATE_TIME AS createTime, " +
            "a.AGENT_NAME AS agentName, a.AGENT_PLATFORM AS agentPlatform, a.AGENT_DESC AS agentDesc, a.AGENT_STATE AS agentState " +
            "FROM tb_project_agent pa JOIN tb_agent a ON pa.AGENT_ID = a.AGENT_ID " +
            "WHERE pa.PROJECT_ID = #{projectId}")
    List<Map<String, Object>> listConfigByProject(Long projectId);
}

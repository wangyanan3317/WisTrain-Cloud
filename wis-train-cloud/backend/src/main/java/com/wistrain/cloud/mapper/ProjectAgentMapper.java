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
    @Select("SELECT pa.*, a.AGENT_NAME, a.AGENT_PLATFORM, a.AGENT_DESC, a.AGENT_STATE " +
            "FROM tb_project_agent pa JOIN tb_agent a ON pa.AGENT_ID = a.AGENT_ID " +
            "WHERE pa.PROJECT_ID = #{projectId}")
    List<Map<String, Object>> listConfigByProject(Long projectId);
}

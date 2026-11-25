package com.wistrain.cloud.mapper;

import com.wistrain.cloud.entity.AgentSession;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface SessionMapper {

    @Insert("INSERT INTO tb_agent_session(AGENT_ID, USER_ID, SESSION_TITLE) VALUES(#{agentId}, #{userId}, #{sessionTitle})")
    @Options(useGeneratedKeys = true, keyProperty = "sessionId")
    int insert(AgentSession s);

    @Update("UPDATE tb_agent_session SET LAST_TIME = NOW() WHERE SESSION_ID = #{id}")
    int touch(Long id);

    @Select("SELECT * FROM tb_agent_session WHERE SESSION_ID = #{id}")
    AgentSession findById(Long id);

    @Select("SELECT * FROM tb_agent_session WHERE AGENT_ID = #{agentId} AND USER_ID = #{userId} ORDER BY LAST_TIME DESC")
    List<AgentSession> listByUserAndAgent(@Param("agentId") Long agentId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM tb_agent_session s JOIN tb_agent a ON s.AGENT_ID = a.AGENT_ID " +
            "WHERE a.DEPT_ID = #{deptId} AND s.LAST_TIME >= #{since}")
    long countSince(@Param("deptId") Long deptId, @Param("since") String since);

    @Select("SELECT DATE(s.LAST_TIME) d, COUNT(*) c FROM tb_agent_session s JOIN tb_agent a ON s.AGENT_ID = a.AGENT_ID " +
            "WHERE a.DEPT_ID = #{deptId} AND s.LAST_TIME >= #{since} GROUP BY DATE(s.LAST_TIME)")
    List<Map<String, Object>> countTrend(@Param("deptId") Long deptId, @Param("since") String since);
}

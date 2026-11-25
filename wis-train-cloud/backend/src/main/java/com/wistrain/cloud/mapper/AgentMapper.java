package com.wistrain.cloud.mapper;

import com.wistrain.cloud.entity.Agent;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AgentMapper {

    @Insert("INSERT INTO tb_agent(AGENT_NAME, AGENT_PLATFORM, AGENT_API_KEY, AGENT_ENDPOINT, AGENT_MODEL, AGENT_DESC, AGENT_STATE, DEPT_ID, CREATE_USER_ID) " +
            "VALUES(#{agentName}, #{agentPlatform}, #{agentApiKey}, #{agentEndpoint}, #{agentModel}, #{agentDesc}, 1, #{deptId}, #{createUserId})")
    @Options(useGeneratedKeys = true, keyProperty = "agentId")
    int insert(Agent a);

    @Update("<script>UPDATE tb_agent<set>" +
            "<if test='agentName != null'>AGENT_NAME = #{agentName},</if>" +
            "<if test='agentEndpoint != null'>AGENT_ENDPOINT = #{agentEndpoint},</if>" +
            "<if test='agentModel != null'>AGENT_MODEL = #{agentModel},</if>" +
            "<if test='agentDesc != null'>AGENT_DESC = #{agentDesc},</if>" +
            "<if test='agentApiKey != null'>AGENT_API_KEY = #{agentApiKey},</if>" +
            "</set>WHERE AGENT_ID = #{agentId}</script>")
    int update(Agent a);

    @Update("UPDATE tb_agent SET AGENT_STATE = #{state} WHERE AGENT_ID = #{id}")
    int updateState(@Param("id") Long id, @Param("state") Integer state);

    @Select("SELECT * FROM tb_agent WHERE AGENT_ID = #{id}")
    Agent findById(Long id);

    @Select("<script>SELECT * FROM tb_agent WHERE DEPT_ID = #{deptId}" +
            "<if test='keyword != null and keyword != \"\"'> AND AGENT_NAME LIKE CONCAT('%',#{keyword},'%')</if>" +
            " ORDER BY AGENT_ID DESC LIMIT #{offset}, #{size}</script>")
    List<Agent> pageByDept(@Param("deptId") Long deptId, @Param("keyword") String keyword,
                           @Param("offset") long offset, @Param("size") int size);

    @Select("<script>SELECT COUNT(*) FROM tb_agent WHERE DEPT_ID = #{deptId}" +
            "<if test='keyword != null and keyword != \"\"'> AND AGENT_NAME LIKE CONCAT('%',#{keyword},'%')</if></script>")
    long countByDept(@Param("deptId") Long deptId, @Param("keyword") String keyword);
}

package com.wistrain.cloud.mapper;

import com.wistrain.cloud.entity.ProjectMember;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface MemberMapper {

    @Insert("INSERT INTO tb_project_member(PROJECT_ID, USER_ID, LEARN_STATE, PROGRESS) VALUES(#{projectId}, #{userId}, 0, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "memberId")
    int insert(ProjectMember m);

    @Select("SELECT * FROM tb_project_member WHERE PROJECT_ID = #{projectId} AND USER_ID = #{userId}")
    ProjectMember findByProjectAndUser(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Update("<script>UPDATE tb_project_member<set>" +
            "<if test='learnState != null'>LEARN_STATE = #{learnState},</if>" +
            "<if test='progress != null'>PROGRESS = #{progress},</if>" +
            "<if test='unlockTime != null'>UNLOCK_TIME = #{unlockTime},</if>" +
            "</set>WHERE MEMBER_ID = #{memberId}</script>")
    int update(ProjectMember m);

    /** 员工「我的项目」：成员记录 + 项目信息（双表，用 Map 承载） */
    @Select("SELECT m.MEMBER_ID AS memberId, m.PROGRESS AS progress, m.LEARN_STATE AS learnState, " +
            "m.UNLOCK_TIME AS unlockTime, m.JOIN_TIME AS joinTime, " +
            "p.PROJECT_ID AS projectId, p.PROJECT_NAME AS projectName, p.PROJECT_DESC AS projectDesc, " +
            "p.PROJECT_STATE AS projectState, p.PROJECT_START AS projectStart, p.PROJECT_END AS projectEnd " +
            "FROM tb_project_member m JOIN tb_training_project p ON m.PROJECT_ID = p.PROJECT_ID " +
            "WHERE m.USER_ID = #{userId} ORDER BY m.MEMBER_ID DESC")
    List<Map<String, Object>> listMy(Long userId);

    /** 项目成员列表（含姓名，US-502） */
    @Select("SELECT m.MEMBER_ID AS memberId, m.PROGRESS AS progress, m.LEARN_STATE AS learnState, " +
            "m.UNLOCK_TIME AS unlockTime, m.JOIN_TIME AS joinTime, " +
            "u.USER_ID AS userId, u.USER_REALNAME AS realname, u.USER_LOGNAME AS logname " +
            "FROM tb_project_member m JOIN tb_user u ON m.USER_ID = u.USER_ID " +
            "WHERE m.PROJECT_ID = #{projectId} ORDER BY m.MEMBER_ID DESC")
    List<Map<String, Object>> listByProject(Long projectId);

    @Delete("DELETE FROM tb_project_member WHERE MEMBER_ID = #{memberId}")
    int deleteById(Long memberId);

    @Select("SELECT COUNT(*) FROM tb_project_member m JOIN tb_training_project p ON m.PROJECT_ID = p.PROJECT_ID " +
            "WHERE p.DEPT_ID = #{deptId}")
    long countByDept(Long deptId);

    /** 近 7 日每日加入人数（趋势图 NF/US-503） */
    @Select("SELECT DATE(m.JOIN_TIME) d, COUNT(*) c FROM tb_project_member m " +
            "JOIN tb_training_project p ON m.PROJECT_ID = p.PROJECT_ID " +
            "WHERE p.DEPT_ID = #{deptId} AND m.JOIN_TIME >= #{since} GROUP BY DATE(m.JOIN_TIME)")
    List<Map<String, Object>> countJoinTrend(@Param("deptId") Long deptId, @Param("since") String since);
}

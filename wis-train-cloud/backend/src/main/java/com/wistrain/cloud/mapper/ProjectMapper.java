package com.wistrain.cloud.mapper;

import com.wistrain.cloud.entity.TrainingProject;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectMapper {

    @Insert("INSERT INTO tb_training_project(PROJECT_NAME, PROJECT_DESC, PROJECT_STATE, PROJECT_START, PROJECT_END, DEPT_ID, CREATE_USER_ID) " +
            "VALUES(#{projectName}, #{projectDesc}, #{projectState}, #{projectStart}, #{projectEnd}, #{deptId}, #{createUserId})")
    @Options(useGeneratedKeys = true, keyProperty = "projectId")
    int insert(TrainingProject p);

    @Update("<script>UPDATE tb_training_project<set>" +
            "<if test='projectName != null'>PROJECT_NAME = #{projectName},</if>" +
            "<if test='projectDesc != null'>PROJECT_DESC = #{projectDesc},</if>" +
            "<if test='projectState != null'>PROJECT_STATE = #{projectState},</if>" +
            "<if test='projectStart != null'>PROJECT_START = #{projectStart},</if>" +
            "<if test='projectEnd != null'>PROJECT_END = #{projectEnd},</if>" +
            "</set>WHERE PROJECT_ID = #{projectId}</script>")
    int update(TrainingProject p);

    @Select("SELECT * FROM tb_training_project WHERE PROJECT_ID = #{id}")
    TrainingProject findById(Long id);

    /** 管理员项目分页（仅本部门，US-202） */
    @Select("<script>SELECT * FROM tb_training_project WHERE DEPT_ID = #{deptId}" +
            "<if test='keyword != null and keyword != \"\"'> AND PROJECT_NAME LIKE CONCAT('%',#{keyword},'%')</if>" +
            "<if test='state != null'> AND PROJECT_STATE = #{state}</if>" +
            " ORDER BY PROJECT_ID DESC LIMIT #{offset}, #{size}</script>")
    List<TrainingProject> pageByDept(@Param("deptId") Long deptId, @Param("keyword") String keyword,
                                     @Param("state") Integer state, @Param("offset") long offset, @Param("size") long size);

    @Select("<script>SELECT COUNT(*) FROM tb_training_project WHERE DEPT_ID = #{deptId}" +
            "<if test='keyword != null and keyword != \"\"'> AND PROJECT_NAME LIKE CONCAT('%',#{keyword},'%')</if>" +
            "<if test='state != null'> AND PROJECT_STATE = #{state}</if></script>")
    long countByDept(@Param("deptId") Long deptId, @Param("keyword") String keyword, @Param("state") Integer state);

    /** 员工可自助加入的项目（US-302）：进行中 + 本部门 + 未加入 */
    @Select("SELECT p.* FROM tb_training_project p WHERE p.PROJECT_STATE = 1 AND p.DEPT_ID = #{deptId} " +
            "AND NOT EXISTS(SELECT 1 FROM tb_project_member m WHERE m.PROJECT_ID = p.PROJECT_ID AND m.USER_ID = #{userId}) " +
            "ORDER BY p.PROJECT_ID DESC")
    List<TrainingProject> listAvailable(@Param("userId") Long userId, @Param("deptId") Long deptId);

    @Select("SELECT COUNT(*) FROM tb_training_project WHERE DEPT_ID = #{deptId}")
    long countAll(Long deptId);

    @Select("SELECT COUNT(*) FROM tb_training_project WHERE DEPT_ID = #{deptId} AND PROJECT_STATE = 1")
    long countActive(Long deptId);
}

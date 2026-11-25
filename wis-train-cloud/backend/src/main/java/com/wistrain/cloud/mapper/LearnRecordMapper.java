package com.wistrain.cloud.mapper;

import com.wistrain.cloud.entity.LearnRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LearnRecordMapper {

    @Insert("INSERT INTO tb_learn_record(PROJECT_ID, USER_ID, MATERIAL_ID) VALUES(#{projectId}, #{userId}, #{materialId})")
    @Options(useGeneratedKeys = true, keyProperty = "recordId")
    int insert(LearnRecord r);

    @Select("SELECT COUNT(*) FROM tb_learn_record WHERE PROJECT_ID = #{projectId} AND USER_ID = #{userId} AND MATERIAL_ID = #{materialId}")
    long exists(@Param("projectId") Long projectId, @Param("userId") Long userId, @Param("materialId") Long materialId);

    @Select("SELECT COUNT(*) FROM tb_learn_record WHERE PROJECT_ID = #{projectId} AND USER_ID = #{userId}")
    long countLearned(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Select("SELECT MATERIAL_ID, LEARN_TIME FROM tb_learn_record WHERE PROJECT_ID = #{projectId} AND USER_ID = #{userId}")
    List<LearnRecord> listByProjectAndUser(@Param("projectId") Long projectId, @Param("userId") Long userId);

    /** 删除资料时同步清理（US-203：该资料学习进度作废） */
    @Delete("DELETE FROM tb_learn_record WHERE MATERIAL_ID = #{materialId}")
    int deleteByMaterial(Long materialId);
}

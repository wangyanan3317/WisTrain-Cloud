package com.wistrain.cloud.mapper;

import com.wistrain.cloud.entity.TrainingMaterial;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MaterialMapper {

    @Insert("INSERT INTO tb_training_material(PROJECT_ID, MATERIAL_NAME, MATERIAL_TYPE, MATERIAL_URL, MATERIAL_ORDER) " +
            "VALUES(#{projectId}, #{materialName}, #{materialType}, #{materialUrl}, #{materialOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "materialId")
    int insert(TrainingMaterial m);

    @Update("UPDATE tb_training_material SET MATERIAL_NAME = #{materialName}, MATERIAL_TYPE = #{materialType}, " +
            "MATERIAL_URL = #{materialUrl}, MATERIAL_ORDER = #{materialOrder} WHERE MATERIAL_ID = #{materialId}")
    int update(TrainingMaterial m);

    @Update("UPDATE tb_training_material SET MATERIAL_ORDER = #{order} WHERE MATERIAL_ID = #{id}")
    int updateOrder(@Param("id") Long id, @Param("order") int order);

    @Delete("DELETE FROM tb_training_material WHERE MATERIAL_ID = #{id}")
    int deleteById(Long id);

    @Select("SELECT * FROM tb_training_material WHERE MATERIAL_ID = #{id}")
    TrainingMaterial findById(Long id);

    @Select("SELECT * FROM tb_training_material WHERE PROJECT_ID = #{projectId} ORDER BY MATERIAL_ORDER, MATERIAL_ID")
    List<TrainingMaterial> listByProject(Long projectId);

    @Select("SELECT COUNT(*) FROM tb_training_material WHERE PROJECT_ID = #{projectId}")
    long countByProject(Long projectId);

    @Select("SELECT COUNT(*) FROM tb_training_material m JOIN tb_training_project p ON m.PROJECT_ID = p.PROJECT_ID WHERE p.DEPT_ID = #{deptId}")
    long countByDept(Long deptId);
}

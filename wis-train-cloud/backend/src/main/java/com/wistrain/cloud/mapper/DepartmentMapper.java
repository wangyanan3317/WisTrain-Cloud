package com.wistrain.cloud.mapper;

import com.wistrain.cloud.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DepartmentMapper {

    @Select("SELECT * FROM tb_department WHERE DEPT_STATE = 1 ORDER BY DEPT_ID")
    List<Department> listAll();

    @Select("SELECT * FROM tb_department WHERE DEPT_ID = #{id}")
    Department findById(Long id);
}

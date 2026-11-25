package com.wistrain.cloud.mapper;

import com.wistrain.cloud.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("SELECT USER_ID AS userId, USER_LOGNAME AS logname, USER_PWD AS pwd, USER_REALNAME AS realname, USER_EMAIL AS email, USER_ROLE AS role, USER_STATE AS state, DEPT_ID AS deptId, CREATE_TIME AS createTime FROM tb_user WHERE USER_LOGNAME = #{logname}")
    User findByLogname(String logname);

    @Select("SELECT USER_ID AS userId, USER_LOGNAME AS logname, USER_PWD AS pwd, USER_REALNAME AS realname, USER_EMAIL AS email, USER_ROLE AS role, USER_STATE AS state, DEPT_ID AS deptId, CREATE_TIME AS createTime FROM tb_user WHERE USER_ID = #{id}")
    User findById(Long id);

    @Insert("INSERT INTO tb_user(USER_LOGNAME, USER_PWD, USER_REALNAME, USER_EMAIL, USER_ROLE, USER_STATE, DEPT_ID) " +
            "VALUES(#{logname}, #{pwd}, #{realname}, #{email}, #{role}, 1, #{deptId})")
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    int insert(User user);

    @Update("UPDATE tb_user SET USER_PWD = #{pwd} WHERE USER_ID = #{id}")
    int updatePwd(@Param("id") Long id, @Param("pwd") String pwd);

    @Update("UPDATE tb_user SET USER_STATE = #{state} WHERE USER_ID = #{id}")
    int updateState(@Param("id") Long id, @Param("state") Integer state);

    /** 管理员分页查询本部门员工（US-501），keyword 模糊匹配登录名/姓名 */
    @Select("<script>SELECT USER_ID AS userId, USER_LOGNAME AS logname, USER_PWD AS pwd, USER_REALNAME AS realname, USER_EMAIL AS email, USER_ROLE AS role, USER_STATE AS state, DEPT_ID AS deptId, CREATE_TIME AS createTime FROM tb_user WHERE USER_ROLE = 2 AND DEPT_ID = #{deptId}" +
            "<if test='keyword != null and keyword != \"\"'> AND (USER_LOGNAME LIKE CONCAT('%',#{keyword},'%') OR USER_REALNAME LIKE CONCAT('%',#{keyword},'%'))</if>" +
            " ORDER BY USER_ID DESC LIMIT #{offset}, #{size}</script>")
    List<User> pageEmployee(@Param("deptId") Long deptId, @Param("keyword") String keyword,
                            @Param("offset") long offset, @Param("size") long size);

    @Select("<script>SELECT COUNT(*) FROM tb_user WHERE USER_ROLE = 2 AND DEPT_ID = #{deptId}" +
            "<if test='keyword != null and keyword != \"\"'> AND (USER_LOGNAME LIKE CONCAT('%',#{keyword},'%') OR USER_REALNAME LIKE CONCAT('%',#{keyword},'%'))</if></script>")
    long countEmployee(@Param("deptId") Long deptId, @Param("keyword") String keyword);

    /** 统计：本部门员工数 */
    @Select("SELECT COUNT(*) FROM tb_user WHERE DEPT_ID = #{deptId} AND USER_ROLE = 2")
    long countByDept(Long deptId);
}

package com.learn.learnbackend.user.repository;

import com.learn.learnbackend.user.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户数据访问层。表名 user 用反引号包裹，避免与 MySQL 的 USER() 函数产生歧义。
 */
@Mapper
public interface UserRepository {

    @Insert("INSERT INTO `user`(username, email, password_hash, status) VALUES(#{username}, #{email}, #{passwordHash}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT * FROM `user` WHERE id = #{id}")
    User findById(@Param("id") Long id);

    @Select("SELECT * FROM `user` WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    @Select("SELECT * FROM `user` WHERE email = #{email}")
    User findByEmail(@Param("email") String email);

    @Select("SELECT * FROM `user` WHERE username = #{login} OR email = #{login} LIMIT 1")
    User findByUsernameOrEmail(@Param("login") String login);

    @Update("UPDATE `user` SET email = #{email} WHERE id = #{id}")
    int updateProfile(User user);

    @Update("UPDATE `user` SET password_hash = #{passwordHash} WHERE id = #{id}")
    int updatePassword(User user);
}

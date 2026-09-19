package com.learn.learnbackend.todo.repository;

import com.learn.learnbackend.todo.entity.Todo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * Todo 数据访问层：只做数据读写，不决定业务流程。
 * 复杂 SQL 可迁移到 src/main/resources/mapper/ 下的 XML。
 */
@Mapper
public interface TodoRepository {

    @Insert("INSERT INTO todo(title, content, done) VALUES(#{title}, #{content}, #{done})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Todo todo);

    @Select("SELECT * FROM todo WHERE id = #{id}")
    Todo findById(@Param("id") Long id);

    @Select("SELECT * FROM todo ORDER BY created_at DESC, id DESC")
    List<Todo> findAll();

    @Update("UPDATE todo SET title = #{title}, content = #{content}, done = #{done} WHERE id = #{id}")
    int update(Todo todo);

    @Delete("DELETE FROM todo WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}

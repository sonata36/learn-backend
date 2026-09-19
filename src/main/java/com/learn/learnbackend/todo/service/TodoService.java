package com.learn.learnbackend.todo.service;

import com.learn.learnbackend.todo.dto.CreateTodoRequest;
import com.learn.learnbackend.todo.dto.TodoResponse;
import com.learn.learnbackend.todo.dto.UpdateTodoRequest;

import java.util.List;

/**
 * Todo 业务接口：只做业务规则，不依赖 HTTP 请求/响应对象。
 */
public interface TodoService {

    TodoResponse create(CreateTodoRequest request);

    List<TodoResponse> list();

    TodoResponse get(Long id);

    TodoResponse update(Long id, UpdateTodoRequest request);

    void delete(Long id);
}

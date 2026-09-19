package com.learn.learnbackend.todo.controller;

import com.learn.learnbackend.common.ApiResponse;
import com.learn.learnbackend.todo.dto.CreateTodoRequest;
import com.learn.learnbackend.todo.dto.TodoResponse;
import com.learn.learnbackend.todo.dto.UpdateTodoRequest;
import com.learn.learnbackend.todo.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Todo 接口层：只负责接参数、校验、调 Service、返回响应，绝不写 SQL。
 * 阶段 2b：注入 CurrentUser，创建时绑定 userId、查询时按 userId 过滤。
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @PostMapping
    public ApiResponse<TodoResponse> create(@Valid @RequestBody CreateTodoRequest request) {
        return ApiResponse.ok(todoService.create(request));
    }

    @GetMapping
    public ApiResponse<List<TodoResponse>> list() {
        return ApiResponse.ok(todoService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<TodoResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(todoService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<TodoResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody UpdateTodoRequest request) {
        return ApiResponse.ok(todoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        todoService.delete(id);
        return ApiResponse.ok();
    }
}

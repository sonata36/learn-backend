package com.learn.learnbackend.todo.service.impl;

import com.learn.learnbackend.common.BusinessException;
import com.learn.learnbackend.common.ErrorCode;
import com.learn.learnbackend.todo.dto.CreateTodoRequest;
import com.learn.learnbackend.todo.dto.TodoResponse;
import com.learn.learnbackend.todo.dto.UpdateTodoRequest;
import com.learn.learnbackend.todo.entity.Todo;
import com.learn.learnbackend.todo.repository.TodoRepository;
import com.learn.learnbackend.todo.service.TodoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TodoServiceImpl implements TodoService {

    private final TodoRepository todoRepository;

    public TodoServiceImpl(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @Override
    @Transactional
    public TodoResponse create(CreateTodoRequest request) {
        Todo todo = new Todo();
        todo.setTitle(request.getTitle());
        todo.setContent(request.getContent());
        todo.setDone(false);
        todoRepository.insert(todo);
        // 回查一次以拿到数据库默认生成的 created_at / updated_at
        return TodoResponse.from(requireById(todo.getId()));
    }

    @Override
    public List<TodoResponse> list() {
        return todoRepository.findAll().stream().map(TodoResponse::from).toList();
    }

    @Override
    public TodoResponse get(Long id) {
        return TodoResponse.from(requireById(id));
    }

    @Override
    @Transactional
    public TodoResponse update(Long id, UpdateTodoRequest request) {
        Todo todo = requireById(id);
        if (request.getTitle() != null) {
            todo.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            todo.setContent(request.getContent());
        }
        if (request.getDone() != null) {
            todo.setDone(request.getDone());
        }
        todoRepository.update(todo);
        return TodoResponse.from(requireById(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireById(id);
        todoRepository.deleteById(id);
    }

    private Todo requireById(Long id) {
        Todo todo = todoRepository.findById(id);
        if (todo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Todo 不存在");
        }
        return todo;
    }
}

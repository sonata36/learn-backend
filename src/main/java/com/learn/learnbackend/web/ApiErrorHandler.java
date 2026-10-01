package com.learn.learnbackend.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** 将常见输入错误转换成可读的 JSON 错误信息。 */
@RestControllerAdvice
public class ApiErrorHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> malformed(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("error", "请求参数或 JSON 格式不正确"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> missing(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("error", error.getReason()));
    }
}

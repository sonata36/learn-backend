package com.learn.learnbackend.todo.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 更新请求：字段全部可选，Service 层对非 null 字段做合并更新。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTodoRequest {

    @Size(max = 100, message = "标题长度不能超过100")
    @Pattern(regexp = "(?sU).*\\S.*", message = "标题不能为空")
    private String title;

    @Size(max = 5000, message = "内容长度不能超过5000")
    private String content;

    private Boolean done;
}

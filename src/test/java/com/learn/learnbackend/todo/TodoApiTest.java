package com.learn.learnbackend.todo;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

/**
 * Todo CRUD 与错误路径。@Transactional 保证每个用例结束回滚，不污染数据库。
 * 运行前需先启动 MySQL：docker compose up -d
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TodoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndGet() throws Exception {
        String created = mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"学习 Spring Boot\",\"content\":\"分层\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("学习 Spring Boot"))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(created).path("data").path("id").asLong();

        mockMvc.perform(get("/api/todos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.content").value("分层"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.done").value(false));

        mockMvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem((int) id)));
    }

    @Test
    void createWithBlankTitleReturns400() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getMissingTodoReturns404() throws Exception {
        mockMvc.perform(get("/api/todos/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void updateAndDelete() throws Exception {
        String created = mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"任务A\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).path("data").path("id").asLong();

        mockMvc.perform(put("/api/todos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("任务A"))
                .andExpect(jsonPath("$.data.done").value(true));

        mockMvc.perform(put("/api/todos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"任务B\",\"content\":\"新内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("任务B"))
                .andExpect(jsonPath("$.data.content").value("新内容"))
                .andExpect(jsonPath("$.data.done").value(true));

        mockMvc.perform(get("/api/todos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("任务B"))
                .andExpect(jsonPath("$.data.content").value("新内容"));

        mockMvc.perform(delete("/api/todos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/todos/{id}", id))
                .andExpect(status().isNotFound());
    }

    static Stream<String> invalidFields() {
        return Stream.of(
                "{\"title\":\"\"}",
                "{\"title\":\"   \"}",
                "{\"title\":\"\\t\\n\"}",
                "{\"title\":\"\\u3000\"}",
                "{\"title\":\"" + "a".repeat(101) + "\"}",
                "{\"title\":\"valid\",\"content\":\"" + "a".repeat(5001) + "\"}");
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void invalidCreateReturns400(String body) throws Exception {
        mockMvc.perform(post("/api/todos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void invalidUpdateDoesNotChangeTodo(String body) throws Exception {
        long id = createTodo();
        mockMvc.perform(put("/api/todos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(get("/api/todos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("original"))
                .andExpect(jsonPath("$.data.done").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"title\":null}", "{", "{\"title\":{}}"})
    void invalidRequestReturns400(String body) throws Exception {
        mockMvc.perform(post("/api/todos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void omittedOrNullUpdateFieldsPreserveExistingValues() throws Exception {
        long id = createTodo();
        mockMvc.perform(put("/api/todos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":null,\"done\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("original"))
                .andExpect(jsonPath("$.data.done").value(true));
    }

    @Test
    void missingTodoCannotBeUpdatedOrDeleted() throws Exception {
        mockMvc.perform(put("/api/todos/{id}", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"done\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(delete("/api/todos/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void invalidPathAndMethodReturnExplicitErrors() throws Exception {
        mockMvc.perform(get("/api/todos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(post("/api/todos/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }

    private long createTodo() throws Exception {
        String body = mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"original\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }
}

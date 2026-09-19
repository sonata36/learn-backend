package com.learn.learnbackend.user;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户注册/登录/资料与安全边界。运行前需先启动 MySQL：docker compose up -d
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserAuthTest {

    private final String suffix = UUID.randomUUID().toString().replace("-", "");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String register(String username, String email) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"password123\"}",
                                username, email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void registerLoginAndMe() throws Exception {
        String username = "alice_" + suffix;
        String email = username + "@example.com";
        String body = register(username, email);
        long id = objectMapper.readTree(body).path("data").path("id").asLong();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"login\":\"%s\",\"password\":\"password123\"}", username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(username));

        // 阶段 2 用 X-User-Id 头临时代表当前用户；阶段 3 替换为真实凭据
        mockMvc.perform(get("/api/users/me").header("X-User-Id", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    void duplicateUsernameReturns409() throws Exception {
        String username = "bob_" + suffix;
        register(username, username + "1@example.com");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"username\":\"%s\",\"email\":\"%s2@example.com\",\"password\":\"password123\"}",
                                username, username)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        String username = "carol_" + suffix;
        register(username, username + "@example.com");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"login\":\"%s\",\"password\":\"wrongpass\"}", username)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void meWithoutLoginReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}

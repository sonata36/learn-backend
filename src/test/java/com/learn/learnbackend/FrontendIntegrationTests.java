package com.learn.learnbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 使用真实 HTTP 端口确认打包后的首页和静态资源能够由 Spring Boot 提供。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:frontend;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "app.console.enabled=false", "spring.sql.init.mode=never"
})
class FrontendIntegrationTests {
    @Value("${local.server.port}")
    private int port;

    @Test
    void servesVersionedPageAndRelativeApiAssets() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> page = get(client, "/");
            assertEquals(200, page.statusCode());
            assertTrue(page.body().contains("STUDIO LEDGER"));
            assertTrue(page.body().contains("V2.1"));

            HttpResponse<String> script = get(client, "/app.js");
            assertEquals(200, script.statusCode());
            assertTrue(script.body().contains("new URL(\"api/\", document.baseURI)"));
            assertEquals(200, get(client, "/styles.css").statusCode());
            assertEquals(200, get(client, "/favicon.svg").statusCode());
        }
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}

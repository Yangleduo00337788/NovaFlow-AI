package ai.novaflow.server.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 知识库 RAG retrieve 集成测试（空知识库也应返回结构化响应）。
 */
@Tag("local")
class KnowledgeRagLocalIntegrationTest extends AbstractLocalIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void retrieveOnEmptyKnowledgeBaseReturnsStructuredResult() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long kbId = OpenApiIntegrationFixtures.createKnowledgeBase(
                restTemplate, session.token(), "RAG-IT-" + suffix);

        Map<String, Object> request = new HashMap<>();
        request.put("query", "integration test");
        request.put("topK", 3);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/knowledge-bases/" + kbId + "/retrieve",
                HttpMethod.POST,
                new HttpEntity<>(request, OpenApiIntegrationFixtures.adminHeaders(session.token())),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(response);
        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertNotNull(data);
        assertTrue(data.containsKey("chunks"), "retrieve result should contain chunks");

        restTemplate.exchange(
                "/api/v1/knowledge-bases/" + kbId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(session.token())),
                Map.class
        );
    }
}

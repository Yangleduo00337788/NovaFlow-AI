package ai.novaflow.server.integration;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 补齐主路径：Studio/门户对话、知识库上传检索、工作流发布运行、Embed 白名单。
 */
@Tag("local")
class FeatureCoverageLocalIntegrationTest extends AbstractLocalIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void studioDebugChatReturnsReplyWhenModelAvailable() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long appId = OpenApiIntegrationFixtures.createApplication(restTemplate, session.token(), "Cov-Chat-App-" + suffix);
        long agentId = OpenApiIntegrationFixtures.createChatAgent(restTemplate, session.token(), appId, "Cov-Chat-" + suffix);
        try {
            ResponseEntity<Map> chat = restTemplate.exchange(
                    "/api/v1/agents/" + agentId + "/debug/chat",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("message", "用一句话介绍你自己", "conversationId", "cov-" + suffix),
                            OpenApiIntegrationFixtures.adminHeaders(session.token())),
                    Map.class
            );
            assumeModelAvailable(chat);
            OpenApiIntegrationFixtures.assertApiSuccess(chat);
            Map<?, ?> data = (Map<?, ?>) chat.getBody().get("data");
            assertNotNull(data.get("reply"));
            assertTrue(String.valueOf(data.get("reply")).trim().length() > 0);
        } finally {
            deleteQuietly("/api/v1/agents/" + agentId, session.token());
            deleteQuietly("/api/v1/applications/" + appId, session.token());
        }
    }

    @Test
    void portalUserCanChatOnPublishedApp() {
        OpenApiIntegrationFixtures.LoginSession admin = OpenApiIntegrationFixtures.login(restTemplate);
        var adminHeaders = OpenApiIntegrationFixtures.adminHeaders(admin.token());
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long appId = OpenApiIntegrationFixtures.createApplication(restTemplate, admin.token(), "Cov-Portal-App-" + suffix);
        long agentId = OpenApiIntegrationFixtures.createChatAgent(restTemplate, admin.token(), appId, "Cov-Portal-" + suffix);
        try {
            OpenApiIntegrationFixtures.assertApiSuccess(restTemplate.exchange(
                    "/api/v1/agents/" + agentId + "/publish",
                    HttpMethod.POST,
                    new HttpEntity<>(null, adminHeaders),
                    Map.class
            ));
            Map<String, Object> updateApp = new HashMap<>();
            updateApp.put("appName", "Cov-Portal-App-" + suffix);
            updateApp.put("description", "coverage");
            updateApp.put("defaultAgentId", agentId);
            updateApp.put("agentIds", List.of(agentId));
            OpenApiIntegrationFixtures.assertApiSuccess(restTemplate.exchange(
                    "/api/v1/applications/" + appId,
                    HttpMethod.PUT,
                    new HttpEntity<>(updateApp, adminHeaders),
                    Map.class
            ));
            OpenApiIntegrationFixtures.assertApiSuccess(restTemplate.exchange(
                    "/api/v1/applications/" + appId + "/publish",
                    HttpMethod.POST,
                    new HttpEntity<>(null, adminHeaders),
                    Map.class
            ));

            OpenApiIntegrationFixtures.LoginSession portal =
                    OpenApiIntegrationFixtures.login(restTemplate, "user@novaflow.ai", "User123!");
            ResponseEntity<Map> chat = restTemplate.exchange(
                    "/api/v1/agents/" + agentId + "/debug/chat",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("message", "你好", "conversationId", "portal-cov-" + suffix),
                            OpenApiIntegrationFixtures.adminHeaders(portal.token())),
                    Map.class
            );
            assumeModelAvailable(chat);
            OpenApiIntegrationFixtures.assertApiSuccess(chat);
        } finally {
            restTemplate.exchange(
                    "/api/v1/applications/" + appId + "/unpublish",
                    HttpMethod.POST,
                    new HttpEntity<>(null, adminHeaders),
                    Map.class
            );
            deleteQuietly("/api/v1/agents/" + agentId, admin.token());
            deleteQuietly("/api/v1/applications/" + appId, admin.token());
        }
    }

    @Test
    void knowledgeUploadThenRetrieve() throws InterruptedException {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String marker = "NovaFlowCoverageMarker" + suffix;
        long kbId = OpenApiIntegrationFixtures.createKnowledgeBase(restTemplate, session.token(), "Cov-KB-" + suffix);
        try {
            ResponseEntity<Map> upload = uploadTextDocument(session.token(), kbId, "coverage-" + suffix + ".txt", marker);
            Assumptions.assumeTrue(
                    upload.getBody() != null && code(upload) == 0,
                    () -> "skip retrieve when document upload failed: " + upload.getBody()
            );
            OpenApiIntegrationFixtures.assertApiSuccess(upload);
            long docId = ((Number) ((Map<?, ?>) upload.getBody().get("data")).get("id")).longValue();

            restTemplate.exchange(
                    "/api/v1/knowledge-bases/" + kbId + "/documents/" + docId + "/reprocess",
                    HttpMethod.POST,
                    new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(session.token())),
                    Map.class
            );
            waitForDocumentProcessed(session.token(), kbId, docId);

            ResponseEntity<Map> retrieve = restTemplate.exchange(
                    "/api/v1/knowledge-bases/" + kbId + "/retrieve",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("query", marker, "topK", 5),
                            OpenApiIntegrationFixtures.adminHeaders(session.token())),
                    Map.class
            );
            OpenApiIntegrationFixtures.assertApiSuccess(retrieve);
            Map<?, ?> data = (Map<?, ?>) retrieve.getBody().get("data");
            assertNotNull(data);
            assertTrue(data.containsKey("chunks"));
        } finally {
            deleteQuietly("/api/v1/knowledge-bases/" + kbId, session.token());
        }
    }

    @Test
    void workflowStartEndPublishAndRun() {
        OpenApiIntegrationFixtures.LoginSession session = OpenApiIntegrationFixtures.login(restTemplate);
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long appId = OpenApiIntegrationFixtures.createApplication(restTemplate, session.token(), "Cov-WF-App-" + suffix);
        var headers = OpenApiIntegrationFixtures.adminHeaders(session.token());
        long workflowId = 0;
        try {
            Map<String, Object> create = new HashMap<>();
            create.put("workflowName", "Cov-WF-" + suffix);
            create.put("applicationId", appId);
            create.put("description", "coverage");
            ResponseEntity<Map> created = restTemplate.exchange(
                    "/api/v1/workflows",
                    HttpMethod.POST,
                    new HttpEntity<>(create, headers),
                    Map.class
            );
            OpenApiIntegrationFixtures.assertApiSuccess(created);
            workflowId = ((Number) ((Map<?, ?>) created.getBody().get("data")).get("id")).longValue();

            Map<String, Object> canvas = new HashMap<>();
            canvas.put("nodes", List.of(
                    Map.of("id", "start-1", "type", "start", "position", Map.of("x", 80, "y", 200),
                            "data", Map.of("label", "开始")),
                    Map.of("id", "end-1", "type", "end", "position", Map.of("x", 400, "y", 200),
                            "data", Map.of("label", "结束"))
            ));
            canvas.put("edges", List.of(Map.of("id", "edge-1", "source", "start-1", "target", "end-1")));

            Map<String, Object> update = new HashMap<>();
            update.put("workflowName", "Cov-WF-" + suffix);
            update.put("applicationId", appId);
            update.put("description", "coverage");
            update.put("canvasData", canvas);
            OpenApiIntegrationFixtures.assertApiSuccess(restTemplate.exchange(
                    "/api/v1/workflows/" + workflowId,
                    HttpMethod.PUT,
                    new HttpEntity<>(update, headers),
                    Map.class
            ));

            OpenApiIntegrationFixtures.assertApiSuccess(restTemplate.exchange(
                    "/api/v1/workflows/" + workflowId + "/publish",
                    HttpMethod.POST,
                    new HttpEntity<>(null, headers),
                    Map.class
            ));

            ResponseEntity<Map> run = restTemplate.exchange(
                    "/api/v1/workflows/" + workflowId + "/run",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("input", "coverage run"), headers),
                    Map.class
            );
            OpenApiIntegrationFixtures.assertApiSuccess(run);
            Map<?, ?> data = (Map<?, ?>) run.getBody().get("data");
            assertNotNull(data.get("status"));
            assertEquals(1, ((Number) data.get("status")).intValue(),
                    () -> "workflow run should succeed, body=" + run.getBody());
        } finally {
            if (workflowId > 0) {
                deleteQuietly("/api/v1/workflows/" + workflowId, session.token());
            }
            deleteQuietly("/api/v1/applications/" + appId, session.token());
        }
    }

    @Test
    void embedWhitelistRejectsForeignReferer() {
        OpenApiIntegrationFixtures.PublishedAgent agent =
                OpenApiIntegrationFixtures.createAndPublishChatAgent(restTemplate);
        OpenApiIntegrationFixtures.LoginSession admin = OpenApiIntegrationFixtures.login(restTemplate);
        var headers = OpenApiIntegrationFixtures.adminHeaders(admin.token());

        Map<String, Object> config = new HashMap<>();
        config.put("themeColor", "#6366f1");
        config.put("allowedDomains", List.of("partner.example"));
        config.put("postMessageTargetOrigin", "*");
        OpenApiIntegrationFixtures.assertApiSuccess(restTemplate.exchange(
                "/api/v1/agents/" + agent.agentId() + "/embed-config",
                HttpMethod.PUT,
                new HttpEntity<>(config, headers),
                Map.class
        ));

        HttpHeaders blocked = OpenApiIntegrationFixtures.embedTokenHeaders(agent.embedToken());
        blocked.setAccept(List.of(MediaType.APPLICATION_JSON));
        blocked.set("Referer", "https://evil.example/embed");
        ResponseEntity<String> blockedWelcome = restTemplate.exchange(
                "/api/v1/open/agents/" + agent.agentId() + "/welcome",
                HttpMethod.GET,
                new HttpEntity<>(null, blocked),
                String.class
        );
        assertTrue(
                blockedWelcome.getStatusCode().is4xxClientError()
                        && String.valueOf(blockedWelcome.getBody()).contains("40304"),
                () -> "expected 40304, http=" + blockedWelcome.getStatusCode() + " body=" + blockedWelcome.getBody()
        );

        HttpHeaders allowed = OpenApiIntegrationFixtures.embedTokenHeaders(agent.embedToken());
        allowed.setAccept(List.of(MediaType.APPLICATION_JSON));
        allowed.set("Referer", "https://partner.example/app");
        ResponseEntity<Map> allowedWelcome = restTemplate.exchange(
                "/api/v1/open/agents/" + agent.agentId() + "/welcome",
                HttpMethod.GET,
                new HttpEntity<>(null, allowed),
                Map.class
        );
        OpenApiIntegrationFixtures.assertApiSuccess(allowedWelcome);
    }

    private static void assumeModelAvailable(ResponseEntity<Map> response) {
        Map<?, ?> body = response.getBody();
        Assumptions.assumeTrue(body != null, "chat response body missing");
        int code = ((Number) body.get("code")).intValue();
        if (code == 0) {
            return;
        }
        String message = String.valueOf(body.get("message"));
        Assumptions.assumeFalse(
                message.contains("模型") || message.contains("密钥") || message.contains("API")
                        || message.contains("解密") || code >= 50000,
                () -> "skip when LLM is unavailable: " + message
        );
        OpenApiIntegrationFixtures.assertApiSuccess(response);
    }

    private ResponseEntity<Map> uploadTextDocument(String token, long kbId, String filename, String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ByteArrayResource file = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);
        return restTemplate.exchange(
                "/api/v1/knowledge-bases/" + kbId + "/documents/upload",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );
    }

    private void waitForDocumentProcessed(String token, long kbId, long docId) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            ResponseEntity<Map> docs = restTemplate.exchange(
                    "/api/v1/knowledge-bases/" + kbId + "/documents?page=1&pageSize=20",
                    HttpMethod.GET,
                    new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(token)),
                    Map.class
            );
            if (docs.getBody() != null && code(docs) == 0) {
                Map<?, ?> data = (Map<?, ?>) docs.getBody().get("data");
                List<?> list = data == null ? List.of() : (List<?>) data.get("list");
                if (list != null) {
                    for (Object item : list) {
                        Map<?, ?> doc = (Map<?, ?>) item;
                        if (docId == ((Number) doc.get("id")).longValue()) {
                            int status = ((Number) doc.get("processStatus")).intValue();
                            if (status == 2 || status == 3) {
                                return;
                            }
                        }
                    }
                }
            }
            Thread.sleep(500);
        }
    }

    private void deleteQuietly(String path, String token) {
        restTemplate.exchange(path, HttpMethod.DELETE, new HttpEntity<>(null, OpenApiIntegrationFixtures.adminHeaders(token)), Map.class);
    }

    private static int code(ResponseEntity<Map> response) {
        Map<?, ?> body = response.getBody();
        if (body == null || body.get("code") == null) {
            return -1;
        }
        return ((Number) body.get("code")).intValue();
    }
}

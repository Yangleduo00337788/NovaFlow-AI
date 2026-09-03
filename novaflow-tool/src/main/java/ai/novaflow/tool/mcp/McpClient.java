package ai.novaflow.tool.mcp;

import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.common.security.UrlSafetyValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class McpClient {

    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration TOOL_CALL_TIMEOUT = Duration.ofMinutes(3);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public McpConnectResult discoverTools(McpServerConfig config) {
        if (config == null) {
            throw new BusinessException("MCP 服务配置不能为空");
        }
        String transportType = StringUtils.hasText(config.getTransportType())
                ? config.getTransportType().trim().toLowerCase()
                : "stdio";

        try {
            return switch (transportType) {
                case "stdio" -> discoverViaStdio(config);
                case "sse" -> discoverViaSse(requireEndpoint(config));
                case "http" -> discoverViaHttp(requireEndpoint(config));
                default -> throw new BusinessException("不支持的传输类型: " + transportType);
            };
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("MCP connect failed: type={}, summary={}, error={}",
                    transportType, config.commandSummary(), ex.getMessage());
            return McpConnectResult.builder()
                    .success(false)
                    .message(ex.getMessage() != null ? ex.getMessage() : "连接失败")
                    .tools(List.of())
                    .build();
        }
    }

    public String callTool(McpServerConfig config, String toolName, Map<String, Object> arguments) {
        if (config == null) {
            throw new BusinessException("MCP 服务配置不能为空");
        }
        if (!StringUtils.hasText(toolName)) {
            throw new BusinessException("MCP 工具名称不能为空");
        }
        String transportType = StringUtils.hasText(config.getTransportType())
                ? config.getTransportType().trim().toLowerCase()
                : "stdio";
        Map<String, Object> args = arguments != null ? arguments : Map.of();
        try {
            JsonNode response = switch (transportType) {
                case "stdio" -> callToolViaStdio(config, toolName, args);
                case "sse", "http" -> callToolViaHttp(requireEndpoint(config), toolName, args);
                default -> throw new BusinessException("不支持的传输类型: " + transportType);
            };
            return parseToolCallResult(response);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("MCP tool call failed: tool={}, summary={}, error={}",
                    toolName, config.commandSummary(), ex.getMessage());
            throw new BusinessException("MCP 工具调用失败: " + ex.getMessage());
        }
    }

    private JsonNode callToolViaStdio(McpServerConfig config, String toolName, Map<String, Object> arguments)
            throws Exception {
        Process process = McpProcessLauncher.start(config);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    process.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8));

            AtomicInteger idSeq = new AtomicInteger(1);
            int initId = idSeq.getAndIncrement();
            sendStdioRequest(writer, initId, "initialize", buildInitializeParams());
            JsonNode initResponse = readStdioResponse(reader, initId, REQUEST_TIMEOUT);
            ensureNoError(initResponse, "initialize");

            sendStdioNotification(writer, "notifications/initialized", Map.of());

            int callId = idSeq.getAndIncrement();
            sendStdioRequest(writer, callId, "tools/call", buildToolCallParams(toolName, arguments));
            JsonNode callResponse = readStdioResponse(reader, callId, TOOL_CALL_TIMEOUT);
            ensureNoError(callResponse, "tools/call");
            return callResponse;
        } finally {
            process.destroy();
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        }
    }

    private JsonNode callToolViaHttp(String endpoint, String toolName, Map<String, Object> arguments) throws Exception {
        AtomicInteger idSeq = new AtomicInteger(1);
        JsonNode initResponse = sendHttpRequest(endpoint, "initialize", buildInitializeParams(), idSeq.getAndIncrement());
        ensureNoError(initResponse, "initialize");

        sendHttpNotification(endpoint, "notifications/initialized", Map.of());

        JsonNode callResponse = sendHttpRequest(
                endpoint,
                "tools/call",
                buildToolCallParams(toolName, arguments),
                idSeq.getAndIncrement());
        ensureNoError(callResponse, "tools/call");
        return callResponse;
    }

    private Map<String, Object> buildToolCallParams(String toolName, Map<String, Object> arguments) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments != null ? arguments : Map.of());
        return params;
    }

    private String parseToolCallResult(JsonNode response) throws Exception {
        JsonNode content = response.path("result").path("content");
        if (!content.isArray() || content.isEmpty()) {
            JsonNode result = response.path("result");
            return result.isMissingNode() || result.isNull()
                    ? response.toString()
                    : objectMapper.writeValueAsString(result);
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode item : content) {
            String type = item.path("type").asText("text");
            if ("text".equals(type)) {
                appendLine(builder, item.path("text").asText(""));
            } else if ("image".equals(type)) {
                appendLine(builder, item.path("data").asText(item.toString()));
            } else {
                appendLine(builder, item.toString());
            }
        }
        return builder.toString();
    }

    private void appendLine(StringBuilder builder, String line) {
        if (!StringUtils.hasText(line)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(line);
    }

    private McpConnectResult discoverViaStdio(McpServerConfig config) throws Exception {
        Process process = McpProcessLauncher.start(config);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    process.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8));

            AtomicInteger idSeq = new AtomicInteger(1);
            int initId = idSeq.getAndIncrement();
            sendStdioRequest(writer, initId, "initialize", buildInitializeParams());
            JsonNode initResponse = readStdioResponse(reader, initId, REQUEST_TIMEOUT);
            ensureNoError(initResponse, "initialize");

            sendStdioNotification(writer, "notifications/initialized", Map.of());

            int listId = idSeq.getAndIncrement();
            sendStdioRequest(writer, listId, "tools/list", Map.of());
            JsonNode listResponse = readStdioResponse(reader, listId, REQUEST_TIMEOUT);
            ensureNoError(listResponse, "tools/list");

            List<McpDiscoveredTool> tools = parseTools(listResponse.path("result"));
            return McpConnectResult.builder()
                    .success(true)
                    .message("连接成功，发现 " + tools.size() + " 个工具")
                    .protocolVersion(PROTOCOL_VERSION)
                    .tools(tools)
                    .build();
        } finally {
            process.destroy();
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        }
    }

    private McpConnectResult discoverViaHttp(String endpoint) throws Exception {
        AtomicInteger idSeq = new AtomicInteger(1);
        JsonNode initResponse = sendHttpRequest(endpoint, "initialize", buildInitializeParams(), idSeq.getAndIncrement());
        ensureNoError(initResponse, "initialize");

        sendHttpNotification(endpoint, "notifications/initialized", Map.of());

        JsonNode listResponse = sendHttpRequest(endpoint, "tools/list", Map.of(), idSeq.getAndIncrement());
        ensureNoError(listResponse, "tools/list");

        List<McpDiscoveredTool> tools = parseTools(listResponse.path("result"));
        return McpConnectResult.builder()
                .success(true)
                .message("连接成功，发现 " + tools.size() + " 个工具")
                .protocolVersion(PROTOCOL_VERSION)
                .tools(tools)
                .build();
    }

    private McpConnectResult discoverViaSse(String endpoint) throws Exception {
        try {
            return discoverViaHttp(endpoint);
        } catch (Exception httpError) {
            log.debug("MCP SSE fallback from HTTP: {}", httpError.getMessage());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new BusinessException("SSE 连接失败: HTTP " + response.statusCode());
        }

        String messageEndpoint = endpoint;
        List<JsonNode> events = parseSsePayload(response.body());
        for (JsonNode event : events) {
            if (event.has("endpoint")) {
                messageEndpoint = event.get("endpoint").asText(messageEndpoint);
            }
        }

        return discoverViaHttp(messageEndpoint);
    }

    private void sendStdioRequest(BufferedWriter writer, int id, String method, Object params) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jsonrpc", "2.0");
        payload.put("id", id);
        payload.put("method", method);
        payload.put("params", params);
        writer.write(objectMapper.writeValueAsString(payload));
        writer.write('\n');
        writer.flush();
    }

    private void sendStdioNotification(BufferedWriter writer, String method, Object params) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jsonrpc", "2.0");
        payload.put("method", method);
        payload.put("params", params);
        writer.write(objectMapper.writeValueAsString(payload));
        writer.write('\n');
        writer.flush();
    }

    private JsonNode readStdioResponse(BufferedReader reader, int expectedId) throws Exception {
        return readStdioResponse(reader, expectedId, REQUEST_TIMEOUT);
    }

    private JsonNode readStdioResponse(BufferedReader reader, int expectedId, Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!reader.ready()) {
                Thread.sleep(50);
                continue;
            }
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String trimmed = line.trim();
            if (!trimmed.startsWith("{")) {
                continue;
            }
            JsonNode node = objectMapper.readTree(trimmed);
            if (node.has("id") && !node.get("id").isNull() && node.get("id").asInt() == expectedId) {
                return node;
            }
        }
        throw new BusinessException("MCP 响应超时");
    }

    private Map<String, Object> buildInitializeParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("protocolVersion", PROTOCOL_VERSION);
        params.put("capabilities", Map.of());
        params.put("clientInfo", Map.of("name", "NovaFlow", "version", "1.0.1"));
        return params;
    }

    private JsonNode sendHttpRequest(String endpoint, String method, Object params, int id) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jsonrpc", "2.0");
        payload.put("id", id);
        payload.put("method", method);
        payload.put("params", params);
        return postPayload(endpoint, payload);
    }

    private void sendHttpNotification(String endpoint, String method, Object params) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jsonrpc", "2.0");
        payload.put("method", method);
        payload.put("params", params);
        postPayload(endpoint, payload);
    }

    private JsonNode postPayload(String endpoint, Map<String, Object> payload) throws Exception {
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new BusinessException("MCP 请求失败: HTTP " + response.statusCode());
        }
        return parseResponseBody(response.body());
    }

    private JsonNode parseResponseBody(String body) throws Exception {
        if (!StringUtils.hasText(body)) {
            throw new BusinessException("MCP 服务返回空响应");
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("event:") || trimmed.contains("\ndata:")) {
            List<JsonNode> events = parseSsePayload(trimmed);
            if (events.isEmpty()) {
                throw new BusinessException("MCP SSE 响应无有效数据");
            }
            return events.get(events.size() - 1);
        }
        return objectMapper.readTree(trimmed);
    }

    private List<JsonNode> parseSsePayload(String body) throws Exception {
        List<JsonNode> nodes = new ArrayList<>();
        String[] lines = body.split("\\R");
        StringBuilder dataBuffer = new StringBuilder();
        for (String line : lines) {
            if (line.startsWith("data:")) {
                if (dataBuffer.length() > 0) {
                    dataBuffer.append('\n');
                }
                dataBuffer.append(line.substring(5).trim());
            } else if (line.isBlank() && dataBuffer.length() > 0) {
                nodes.add(objectMapper.readTree(dataBuffer.toString()));
                dataBuffer.setLength(0);
            }
        }
        if (dataBuffer.length() > 0) {
            nodes.add(objectMapper.readTree(dataBuffer.toString()));
        }
        return nodes;
    }

    private void ensureNoError(JsonNode response, String step) {
        if (response == null) {
            throw new BusinessException("MCP " + step + " 无响应");
        }
        JsonNode error = response.get("error");
        if (error != null && !error.isNull()) {
            String message = error.path("message").asText("未知错误");
            throw new BusinessException("MCP " + step + " 失败: " + message);
        }
    }

    @SuppressWarnings("unchecked")
    private List<McpDiscoveredTool> parseTools(JsonNode resultNode) {
        JsonNode toolsNode = resultNode.path("tools");
        if (!toolsNode.isArray()) {
            return List.of();
        }
        List<McpDiscoveredTool> tools = new ArrayList<>();
        for (JsonNode toolNode : toolsNode) {
            String name = toolNode.path("name").asText("");
            if (!StringUtils.hasText(name)) {
                continue;
            }
            Map<String, Object> inputSchema = null;
            JsonNode schemaNode = toolNode.get("inputSchema");
            if (schemaNode != null && !schemaNode.isNull()) {
                inputSchema = objectMapper.convertValue(schemaNode, Map.class);
            }
            tools.add(McpDiscoveredTool.builder()
                    .name(name)
                    .description(toolNode.path("description").asText(null))
                    .inputSchema(inputSchema)
                    .build());
        }
        return tools;
    }

    private String requireEndpoint(McpServerConfig config) {
        if (!StringUtils.hasText(config.getEndpoint())) {
            throw new BusinessException("远程 MCP 配置缺少 url/endpoint 字段");
        }
        String endpoint = config.getEndpoint().trim();
        UrlSafetyValidator.validateHttpUrl(endpoint);
        return endpoint;
    }
}

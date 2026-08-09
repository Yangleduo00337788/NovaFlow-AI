package ai.novaflow.tool.executor;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.common.exception.BusinessException;
import ai.novaflow.tool.domain.HttpToolDefinition;
import ai.novaflow.tool.entity.McpServerEntity;
import ai.novaflow.tool.mapper.McpServerMapper;
import ai.novaflow.tool.mcp.McpClient;
import ai.novaflow.tool.mcp.McpServerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class McpToolExecutor {

    private final McpServerMapper mcpServerMapper;
    private final McpClient mcpClient;
    private final ObjectMapper objectMapper;

    public String execute(HttpToolDefinition tool, Map<String, Object> arguments) {
        if (tool == null || tool.getMcpServerId() == null || !StringUtils.hasText(tool.getMcpToolName())) {
            throw new BusinessException("MCP 工具配置不完整");
        }
        McpServerEntity server = mcpServerMapper.selectOneById(tool.getMcpServerId());
        if (server == null || server.getIsDeleted() != null && server.getIsDeleted() != 0) {
            throw new BusinessException("MCP 服务不存在");
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.equals(server.getTenantId())) {
            throw new BusinessException("MCP 服务不存在");
        }
        McpServerConfig config = McpServerConfig.parse(objectMapper, server.getServerName(), server.getServerConfig());
        return mcpClient.callTool(config, tool.getMcpToolName(), arguments);
    }
}

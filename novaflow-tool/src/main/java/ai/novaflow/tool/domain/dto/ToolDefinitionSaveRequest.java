package ai.novaflow.tool.domain.dto;

import ai.novaflow.tool.domain.HttpToolDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ToolDefinitionSaveRequest {

    @NotBlank(message = "工具标识不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]{1,62}$", message = "工具标识需以小写字母开头，仅含小写字母、数字和下划线")
    private String toolName;

    @NotBlank(message = "显示名称不能为空")
    private String displayName;

    private String description;

    private String toolType = "http";

    private String method = "GET";

    @NotBlank(message = "请求 URL 不能为空")
    private String url;
    private String bodyTemplate;
    private java.util.Map<String, String> headers;
    private java.util.Map<String, Object> inputSchema;

    public HttpToolDefinition toHttpToolDefinition() {
        HttpToolDefinition tool = new HttpToolDefinition();
        tool.setName(toolName);
        tool.setDescription(description);
        tool.setMethod(method);
        tool.setUrl(url);
        tool.setBodyTemplate(bodyTemplate);
        tool.setHeaders(headers);
        tool.setInputSchema(inputSchema);
        return tool;
    }
}

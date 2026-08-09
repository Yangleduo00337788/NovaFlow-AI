package ai.novaflow.tool.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class McpServerSaveRequest {

    @NotBlank(message = "服务名称不能为空")
    private String serverName;
    private String description;
    @NotBlank(message = "传输类型不能为空")
    private String transportType;
    @NotBlank(message = "服务地址不能为空")
    private String endpoint;
}

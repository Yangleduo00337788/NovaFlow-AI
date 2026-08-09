package ai.novaflow.tool.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("mcp_server")
public class McpServerEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private String serverName;
    private String description;
    private String transportType;
    @Column("server_config")
    private String serverConfig;
    @Column("discovered_tools")
    private String discoveredTools;
    private Integer status;
    private LocalDateTime lastConnectedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}

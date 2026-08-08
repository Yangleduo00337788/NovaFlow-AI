package ai.novaflow.tool.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("tool_definition")
public class ToolDefinitionEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private String toolName;
    private String displayName;
    private String description;
    private String toolType;
    @Column("tool_config")
    private String toolConfig;
    private Integer isEnabled;
    private Integer isPublic;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}

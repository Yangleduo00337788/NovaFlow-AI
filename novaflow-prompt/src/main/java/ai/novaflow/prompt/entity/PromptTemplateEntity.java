package ai.novaflow.prompt.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("prompt_template")
public class PromptTemplateEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private String templateName;
    private String description;
    private String category;
    private String content;
    private String variables;
    private String visibility;
    private Integer currentVersion;
    private Integer usageCount;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDeleted;
}

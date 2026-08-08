package ai.novaflow.prompt.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("prompt_template_version")
public class PromptTemplateVersionEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long templateId;
    private Integer version;
    private String content;
    private String variables;
    private String changeLog;
    private Long publishedBy;
    private LocalDateTime publishedAt;
}

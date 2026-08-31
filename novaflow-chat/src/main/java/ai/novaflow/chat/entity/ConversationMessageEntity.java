package ai.novaflow.chat.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("conversation_message")
public class ConversationMessageEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long conversationId;
    private String role;
    private String content;
    private Integer tokensUsed;
    private Long latencyMs;
    private String retrievalSources;
    private LocalDateTime createdAt;
}

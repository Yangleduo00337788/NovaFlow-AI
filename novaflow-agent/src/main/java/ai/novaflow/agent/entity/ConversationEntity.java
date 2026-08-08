package ai.novaflow.agent.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("conversation")
public class ConversationEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long agentId;
    private String conversationKey;
    private String channel;
    private Long userId;
    private Integer messageCount;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

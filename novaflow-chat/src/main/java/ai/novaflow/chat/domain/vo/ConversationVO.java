package ai.novaflow.chat.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConversationVO {

    private Long id;
    private String conversationKey;
    private String channel;
    private Integer messageCount;
    private String preview;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
}

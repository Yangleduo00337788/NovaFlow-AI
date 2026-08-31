package ai.novaflow.chat.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ConversationMessageVO {

    private Long id;
    private String role;
    private String content;
    private Integer tokensUsed;
    private Long latencyMs;
    private List<RetrievalSourceVO> sources;
    private LocalDateTime createdAt;
}

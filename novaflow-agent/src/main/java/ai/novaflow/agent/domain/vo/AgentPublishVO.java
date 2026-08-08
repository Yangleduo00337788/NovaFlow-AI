package ai.novaflow.agent.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AgentPublishVO {

    private Long agentId;
    private Integer status;
    private Integer version;
    private LocalDateTime publishedAt;
    private String apiKeyPrefix;
    /** 仅在发布或轮换密钥时返回一次 */
    private String apiKey;
    private String chatEndpoint;
    private String streamEndpoint;
    private String welcomeEndpoint;
    private String embedPath;
}

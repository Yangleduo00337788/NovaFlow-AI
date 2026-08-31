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
    /** 仅在发布或轮换密钥时返回一次（服务端集成用） */
    private String apiKey;
    private String embedTokenPrefix;
    /** 仅在发布或轮换嵌入 Token 时返回一次（网页嵌入用，权限受限） */
    private String embedToken;
    private String chatEndpoint;
    private String streamEndpoint;
    private String welcomeEndpoint;
    private String embedPath;
}

package ai.novaflow.agent.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("agent_knowledge")
public class AgentKnowledgeEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long agentId;
    private Long knowledgeBaseId;
    private LocalDateTime createdAt;
}

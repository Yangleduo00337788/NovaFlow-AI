package ai.novaflow.agent.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("agent_skill")
public class AgentSkillEntity {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long tenantId;
    private Long agentId;
    private Long skillId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}

package ai.novaflow.knowledge.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DocumentProcessEvent extends ApplicationEvent {

    private final Long documentId;
    private final Long knowledgeBaseId;
    private final Long tenantId;

    public DocumentProcessEvent(Object source, Long documentId, Long knowledgeBaseId, Long tenantId) {
        super(source);
        this.documentId = documentId;
        this.knowledgeBaseId = knowledgeBaseId;
        this.tenantId = tenantId;
    }
}

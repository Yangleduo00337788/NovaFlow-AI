package ai.novaflow.rag.pipeline;

import ai.novaflow.common.context.TenantContext;
import ai.novaflow.knowledge.event.DocumentProcessEvent;
import ai.novaflow.knowledge.event.DocumentVectorDeleteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessListener {

    private final DocumentProcessingService documentProcessingService;

    @Async("documentProcessExecutor")
    @EventListener
    public void onDocumentUploaded(DocumentProcessEvent event) {
        log.info("Start processing document: {}", event.getDocumentId());
        runWithTenant(event.getTenantId(), () ->
                documentProcessingService.process(event.getDocumentId(), event.getKnowledgeBaseId(), event.getTenantId()));
    }

    @Async("documentProcessExecutor")
    @EventListener
    public void onDocumentDeleted(DocumentVectorDeleteEvent event) {
        log.info("Delete vectors for document: {}", event.getDocumentId());
        runWithTenant(event.getTenantId(), () ->
                documentProcessingService.deleteVectors(event.getDocumentId(), event.getKnowledgeBaseId(), event.getTenantId()));
    }

    private void runWithTenant(Long tenantId, Runnable action) {
        try {
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
            action.run();
        } finally {
            TenantContext.clear();
        }
    }
}

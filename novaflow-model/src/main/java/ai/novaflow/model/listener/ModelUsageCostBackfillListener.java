package ai.novaflow.model.listener;

import ai.novaflow.common.event.TokenUsageRecordedEvent;
import ai.novaflow.model.service.ModelUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModelUsageCostBackfillListener {

    private static final int BACKFILL_BATCH_SIZE = 100;

    private final ModelUsageService modelUsageService;

    @Async
    @EventListener
    public void onTokenUsageRecorded(TokenUsageRecordedEvent event) {
        if (event == null || event.tenantId() == null) {
            return;
        }
        modelUsageService.backfillMissingCosts(event.tenantId(), BACKFILL_BATCH_SIZE);
    }
}

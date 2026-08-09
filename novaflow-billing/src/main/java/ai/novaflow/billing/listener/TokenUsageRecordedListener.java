package ai.novaflow.billing.listener;

import ai.novaflow.billing.service.BillingAlertService;
import ai.novaflow.common.event.TokenUsageRecordedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenUsageRecordedListener {

    private final BillingAlertService billingAlertService;

    @EventListener
    public void onTokenUsageRecorded(TokenUsageRecordedEvent event) {
        if (event == null || event.tenantId() == null) {
            return;
        }
        billingAlertService.checkAlerts(event.tenantId());
    }
}

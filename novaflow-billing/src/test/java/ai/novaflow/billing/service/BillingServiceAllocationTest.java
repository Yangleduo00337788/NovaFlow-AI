package ai.novaflow.billing.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BillingServiceAllocationTest {

    @Test
    void tokenPercentRoundsAgainstTotal() {
        assertEquals(0, BillingService.tokenPercent(10, 0));
        assertEquals(0, BillingService.tokenPercent(0, 100));
        assertEquals(25, BillingService.tokenPercent(25, 100));
        assertEquals(33, BillingService.tokenPercent(1, 3));
        assertEquals(100, BillingService.tokenPercent(100, 100));
    }
}

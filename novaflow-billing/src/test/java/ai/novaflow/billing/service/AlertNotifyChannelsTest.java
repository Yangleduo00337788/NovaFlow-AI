package ai.novaflow.billing.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlertNotifyChannelsTest {

    @Test
    void defaultsToSiteAndDropsUnknown() {
        assertEquals(List.of("site"), AlertNotifyChannels.normalize(null));
        assertEquals(List.of("site", "email", "webhook"), AlertNotifyChannels.normalize(
                List.of("site", "email", "webhook", "sms", "EMAIL")));
        assertEquals("site,email", AlertNotifyChannels.join(List.of("site", "email")));
    }
}

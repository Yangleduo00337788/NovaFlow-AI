package ai.novaflow.user.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlatformAlertChannelsTest {

    @Test
    void normalizeEmpty() {
        assertEquals(List.of(), PlatformAlertChannels.normalize(null));
        assertEquals(List.of(), PlatformAlertChannels.normalize(""));
    }

    @Test
    void normalizeAndJoin() {
        assertEquals(List.of("webhook", "email"), PlatformAlertChannels.normalize("webhook,email,invalid"));
        assertEquals("webhook,email", PlatformAlertChannels.join(List.of("webhook", "email", "email")));
    }
}

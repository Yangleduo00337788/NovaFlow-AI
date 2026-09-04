package ai.novaflow.common.util;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicHttpUrlsTest {

    @Test
    void rejectsPrivateAndLocalTargets() {
        assertFalse(PublicHttpUrls.isSafeWebhookUrl("http://127.0.0.1/hook"));
        assertFalse(PublicHttpUrls.isSafeWebhookUrl("http://localhost/hook"));
        assertFalse(PublicHttpUrls.isSafeWebhookUrl("http://10.0.0.8/hook"));
        assertFalse(PublicHttpUrls.isSafeWebhookUrl("http://[fd12:3456:789a::1]/hook"));
        assertFalse(PublicHttpUrls.isSafeWebhookUrl("not-a-url"));
        assertFalse(PublicHttpUrls.isSafeWebhookUrl("ftp://example.com/hook"));
    }

    @Test
    void loopbackInetIsBlocked() throws Exception {
        assertTrue(PublicHttpUrls.isBlockedAddress(InetAddress.getByName("127.0.0.1")));
    }
}

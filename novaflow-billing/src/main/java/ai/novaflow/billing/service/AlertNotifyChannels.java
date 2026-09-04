package ai.novaflow.billing.service;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class AlertNotifyChannels {

    static final Set<String> ALLOWED = Set.of("site", "email", "webhook");

    private AlertNotifyChannels() {
    }

    static List<String> normalize(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return List.of("site");
        }
        List<String> normalized = new ArrayList<>();
        for (String channel : channels) {
            if (!StringUtils.hasText(channel)) {
                continue;
            }
            String code = channel.trim().toLowerCase(Locale.ROOT);
            if (ALLOWED.contains(code) && !normalized.contains(code)) {
                normalized.add(code);
            }
        }
        return normalized.isEmpty() ? List.of("site") : List.copyOf(normalized);
    }

    static String join(List<String> channels) {
        return String.join(",", normalize(channels));
    }
}

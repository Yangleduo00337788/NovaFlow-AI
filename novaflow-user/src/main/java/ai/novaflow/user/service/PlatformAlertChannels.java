package ai.novaflow.user.service;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class PlatformAlertChannels {

    private static final Set<String> ALLOWED = Set.of("email", "webhook");

    private PlatformAlertChannels() {
    }

    static List<String> normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        LinkedHashSet<String> channels = new LinkedHashSet<>();
        for (String item : raw.split(",")) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            String normalized = item.trim().toLowerCase(Locale.ROOT);
            if (ALLOWED.contains(normalized)) {
                channels.add(normalized);
            }
        }
        return new ArrayList<>(channels);
    }

    static List<String> normalizeList(List<String> channels) {
        if (channels == null || channels.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String channel : channels) {
            if (!StringUtils.hasText(channel)) {
                continue;
            }
            String code = channel.trim().toLowerCase(Locale.ROOT);
            if (ALLOWED.contains(code)) {
                normalized.add(code);
            }
        }
        return new ArrayList<>(normalized);
    }

    static String join(List<String> channels) {
        return String.join(",", normalizeList(channels));
    }
}

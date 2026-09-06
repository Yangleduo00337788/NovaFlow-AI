package ai.novaflow.user.service;

import ai.novaflow.user.entity.PlatformSystemConfigEntity;
import ai.novaflow.user.mapper.PlatformSystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformSystemConfigService {

    public static final String KEY_REGISTRATION_ENABLED = "auth.registration_enabled";
    public static final String KEY_HOURLY_CALLS_THRESHOLD = "api_monitor.hourly_calls_threshold";
    public static final String KEY_TRAFFIC_SPIKE_MULTIPLIER = "api_monitor.traffic_spike_multiplier";
    public static final String KEY_ALLOWED_PROVIDER_CODES = "model.allowed_provider_codes";
    public static final String KEY_MAINTENANCE_ENABLED = "platform.maintenance_enabled";
    public static final String KEY_MAINTENANCE_MESSAGE = "platform.maintenance_message";
    public static final String KEY_PLATFORM_ANNOUNCEMENT = "platform.announcement";
    public static final String KEY_RISK_ABNORMAL_LOGIN_ENABLED = "risk.abnormal_login_enabled";
    public static final String KEY_RISK_BATCH_REGISTER_IP_LIMIT = "risk.batch_register_ip_limit_per_day";
    public static final String KEY_RISK_NEW_USER_AGENT_ENABLED = "risk.new_user_agent_enabled";
    public static final String KEY_RISK_STORAGE_WARN_PERCENT = "risk.storage_warn_percent";

    private final PlatformSystemConfigMapper configMapper;

    @Value("${novaflow.auth.registration-enabled:true}")
    private boolean defaultRegistrationEnabled;

    @Value("${novaflow.platform.api-monitor.hourly-calls-threshold:500}")
    private long defaultHourlyCallsThreshold;

    @Value("${novaflow.platform.api-monitor.traffic-spike-multiplier:3}")
    private double defaultTrafficSpikeMultiplier;

    @Value("${novaflow.platform.risk.batch-register-ip-limit-per-day:5}")
    private int defaultBatchRegisterIpLimitPerDay;

    @Value("${novaflow.platform.risk.storage-warn-percent:80}")
    private int defaultStorageWarnPercent;

    public boolean isRegistrationEnabled() {
        return parseBoolean(getValue(KEY_REGISTRATION_ENABLED), defaultRegistrationEnabled);
    }

    public void setRegistrationEnabled(boolean enabled, Long operatorId) {
        upsert(KEY_REGISTRATION_ENABLED, Boolean.toString(enabled), operatorId);
    }

    public long getHourlyCallsThreshold() {
        return parseLong(getValue(KEY_HOURLY_CALLS_THRESHOLD), defaultHourlyCallsThreshold);
    }

    public void setHourlyCallsThreshold(long threshold, Long operatorId) {
        upsert(KEY_HOURLY_CALLS_THRESHOLD, Long.toString(Math.max(1, threshold)), operatorId);
    }

    public double getTrafficSpikeMultiplier() {
        return parseDouble(getValue(KEY_TRAFFIC_SPIKE_MULTIPLIER), defaultTrafficSpikeMultiplier);
    }

    public void setTrafficSpikeMultiplier(double multiplier, Long operatorId) {
        upsert(KEY_TRAFFIC_SPIKE_MULTIPLIER, Double.toString(Math.max(1.0, multiplier)), operatorId);
    }

    /**
     * @return 空集合表示未配置白名单（全部允许）
     */
    public Set<String> getAllowedProviderCodes() {
        String raw = getValue(KEY_ALLOWED_PROVIDER_CODES);
        if (!StringUtils.hasText(raw)) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(code -> code.trim().toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void setAllowedProviderCodes(Set<String> providerCodes, Long operatorId) {
        if (providerCodes == null || providerCodes.isEmpty()) {
            upsert(KEY_ALLOWED_PROVIDER_CODES, "", operatorId);
            return;
        }
        String value = providerCodes.stream()
                .map(code -> code.trim().toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(","));
        upsert(KEY_ALLOWED_PROVIDER_CODES, value, operatorId);
    }

    public boolean isMaintenanceEnabled() {
        return parseBoolean(getValue(KEY_MAINTENANCE_ENABLED), false);
    }

    public void setMaintenanceEnabled(boolean enabled, Long operatorId) {
        upsert(KEY_MAINTENANCE_ENABLED, Boolean.toString(enabled), operatorId);
    }

    public String getMaintenanceMessage() {
        String value = getValue(KEY_MAINTENANCE_MESSAGE);
        return value != null ? value : "";
    }

    public void setMaintenanceMessage(String message, Long operatorId) {
        upsert(KEY_MAINTENANCE_MESSAGE, message != null ? message : "", operatorId);
    }

    public String getPlatformAnnouncement() {
        String value = getValue(KEY_PLATFORM_ANNOUNCEMENT);
        return value != null ? value : "";
    }

    public void setPlatformAnnouncement(String announcement, Long operatorId) {
        upsert(KEY_PLATFORM_ANNOUNCEMENT, announcement != null ? announcement : "", operatorId);
    }

    public boolean isAbnormalLoginEnabled() {
        return parseBoolean(getValue(KEY_RISK_ABNORMAL_LOGIN_ENABLED), true);
    }

    public void setAbnormalLoginEnabled(boolean enabled, Long operatorId) {
        upsert(KEY_RISK_ABNORMAL_LOGIN_ENABLED, Boolean.toString(enabled), operatorId);
    }

    public boolean isNewUserAgentEnabled() {
        return parseBoolean(getValue(KEY_RISK_NEW_USER_AGENT_ENABLED), true);
    }

    public void setNewUserAgentEnabled(boolean enabled, Long operatorId) {
        upsert(KEY_RISK_NEW_USER_AGENT_ENABLED, Boolean.toString(enabled), operatorId);
    }

    public int getBatchRegisterIpLimitPerDay() {
        return (int) parseLong(getValue(KEY_RISK_BATCH_REGISTER_IP_LIMIT), defaultBatchRegisterIpLimitPerDay);
    }

    public void setBatchRegisterIpLimitPerDay(int limit, Long operatorId) {
        upsert(KEY_RISK_BATCH_REGISTER_IP_LIMIT, Integer.toString(Math.max(0, limit)), operatorId);
    }

    public int getStorageWarnPercent() {
        return (int) parseLong(getValue(KEY_RISK_STORAGE_WARN_PERCENT), defaultStorageWarnPercent);
    }

    public void setStorageWarnPercent(int percent, Long operatorId) {
        int normalized = Math.min(100, Math.max(50, percent));
        upsert(KEY_RISK_STORAGE_WARN_PERCENT, Integer.toString(normalized), operatorId);
    }

    private String getValue(String key) {
        PlatformSystemConfigEntity entity = configMapper.selectOneById(key);
        if (entity == null || entity.getConfigValue() == null) {
            return null;
        }
        return entity.getConfigValue();
    }

    private void upsert(String key, String value, Long operatorId) {
        PlatformSystemConfigEntity existing = configMapper.selectOneById(key);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            PlatformSystemConfigEntity entity = new PlatformSystemConfigEntity();
            entity.setConfigKey(key);
            entity.setConfigValue(value);
            entity.setUpdatedAt(now);
            entity.setUpdatedBy(operatorId);
            configMapper.insert(entity);
            return;
        }
        existing.setConfigValue(value);
        existing.setUpdatedAt(now);
        existing.setUpdatedBy(operatorId);
        configMapper.update(existing);
    }

    private boolean parseBoolean(String value, boolean defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim());
    }

    private long parseLong(String value, long defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private double parseDouble(String value, double defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}

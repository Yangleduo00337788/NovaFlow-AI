package ai.novaflow.user.service;

import ai.novaflow.common.security.MaintenanceModeChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlatformMaintenanceChecker implements MaintenanceModeChecker {

    private final PlatformSystemConfigService platformSystemConfigService;

    @Override
    public boolean isMaintenanceEnabled() {
        return platformSystemConfigService.isMaintenanceEnabled();
    }

    @Override
    public String getMaintenanceMessage() {
        String message = platformSystemConfigService.getMaintenanceMessage();
        return message != null && !message.isBlank()
                ? message.trim()
                : "系统维护中，请稍后再试";
    }
}

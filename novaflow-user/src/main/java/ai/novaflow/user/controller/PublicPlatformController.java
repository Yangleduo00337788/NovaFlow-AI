package ai.novaflow.user.controller;

import ai.novaflow.common.domain.ApiResult;
import ai.novaflow.user.domain.vo.PublicPlatformStatusVO;
import ai.novaflow.user.service.PlatformSystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicPlatformController {

    private final PlatformSystemConfigService platformSystemConfigService;

    @GetMapping("/platform-status")
    public ApiResult<PublicPlatformStatusVO> platformStatus() {
        return ApiResult.ok(PublicPlatformStatusVO.builder()
                .maintenanceEnabled(platformSystemConfigService.isMaintenanceEnabled())
                .maintenanceMessage(platformSystemConfigService.getMaintenanceMessage())
                .platformAnnouncement(platformSystemConfigService.getPlatformAnnouncement())
                .build());
    }
}

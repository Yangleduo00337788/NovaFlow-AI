package ai.novaflow.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlatformSecurityOverviewVO {

    private long openAlertCount;
    private long abnormalLoginOpenCount;
    private long batchRegisterOpenCount;
    private long newUserAgentOpenCount;
}

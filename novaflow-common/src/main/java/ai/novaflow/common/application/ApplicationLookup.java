package ai.novaflow.common.application;

import java.util.List;
import java.util.Map;

/**
 * 供工作流等模块校验应用归属、解析应用名称，避免 workflow 直接依赖 application。
 */
public interface ApplicationLookup {

    void requireExists(Long tenantId, Long applicationId);

    Map<Long, String> getApplicationNameMap(List<Long> applicationIds);
}

package ai.novaflow.agent.util;

import ai.novaflow.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public final class OpenApiCallerIdValidator {

    private static final Pattern CALLER_ID_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{7,127}$");

    private OpenApiCallerIdValidator() {
    }

    public static String requireValid(String callerId) {
        if (!StringUtils.hasText(callerId)) {
            throw new BusinessException(40001, "缺少终端用户标识 X-Caller-Id");
        }
        String trimmed = callerId.trim();
        if (!CALLER_ID_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException(40001, "X-Caller-Id 格式无效（8-128 位字母数字及 ._:-）");
        }
        return trimmed;
    }
}

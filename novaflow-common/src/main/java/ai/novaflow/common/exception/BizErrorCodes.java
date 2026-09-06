package ai.novaflow.common.exception;

/**
 * 业务错误码：与 {@link BusinessException} 配合使用，便于前端区分场景。
 */
public final class BizErrorCodes {

    public static final int STORAGE_QUOTA_EXCEEDED = 40036;

    private BizErrorCodes() {
    }
}

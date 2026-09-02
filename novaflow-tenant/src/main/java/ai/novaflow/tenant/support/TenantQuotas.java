package ai.novaflow.tenant.support;

import ai.novaflow.common.exception.BusinessException;

/**
 * 套餐配额硬校验工具：业务模块在创建资源或执行对话前调用，
 * 超出租户套餐限制时直接抛出业务异常。
 */
public final class TenantQuotas {

    private TenantQuotas() {
    }

    /**
     * 读取配额上限，未配置或非法值时返回兜底值。
     */
    public static int limit(Integer configured, int fallback) {
        return configured != null && configured > 0 ? configured : fallback;
    }

    /**
     * 校验资源数量是否已达上限，超出则抛业务异常。
     *
     * @param resourceLabel 资源名称，如 "Agent"、"知识库"
     * @param used          当前已用数量（不含本次）
     * @param limit         套餐上限
     */
    public static void assertWithinLimit(String resourceLabel, long used, int limit) {
        if (used >= limit) {
            throw new BusinessException(
                    resourceLabel + "数量已达套餐上限（" + used + "/" + limit + "），请升级套餐");
        }
    }

    /**
     * 校验存储用量：已用 + 本次上传超过上限则抛业务异常。
     *
     * @param usedBytes    租户现存文档总字节数
     * @param incomingBytes 本次上传文件字节数
     * @param limitBytes   套餐存储上限字节数（<=0 视为不限制）
     */
    public static void assertStorageWithinLimit(long usedBytes, long incomingBytes, long limitBytes) {
        if (limitBytes <= 0) {
            return;
        }
        if (usedBytes + incomingBytes > limitBytes) {
            throw new BusinessException(String.format(
                    "存储空间超出套餐上限（已用 %.1fMB / 上限 %.0fMB），请升级套餐或清理文档",
                    usedBytes / 1024.0 / 1024.0,
                    limitBytes / 1024.0 / 1024.0));
        }
    }
}

package ai.novaflow.common.security;

/**
 * 用户账号域：租户企业账号与平台运营账号分离。
 */
public final class AccountTypes {

    public static final String TENANT = "tenant";
    public static final String PLATFORM = "platform";

    private AccountTypes() {
    }

    public static boolean isPlatform(String accountType) {
        return PLATFORM.equals(accountType);
    }
}

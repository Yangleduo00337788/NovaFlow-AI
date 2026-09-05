package ai.novaflow.user.support;

import java.security.SecureRandom;

public final class OnboardingPasswordGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";

    private OnboardingPasswordGenerator() {
    }

    public static String generate() {
        StringBuilder password = new StringBuilder();
        password.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        password.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        String all = LETTERS + DIGITS;
        for (int i = 0; i < 10; i++) {
            password.append(all.charAt(RANDOM.nextInt(all.length())));
        }
        return password.toString();
    }
}

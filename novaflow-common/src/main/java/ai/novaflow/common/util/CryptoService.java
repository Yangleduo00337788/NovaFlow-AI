package ai.novaflow.common.util;

import ai.novaflow.common.config.CryptoProperties;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class CryptoService {

    private final CryptoProperties cryptoProperties;

    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        return aes().encryptBase64(plainText);
    }

    public String decrypt(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return null;
        }
        return aes().decryptStr(cipherText);
    }

    /**
     * 解密失败时返回 null，避免因历史密钥不一致导致接口 500。
     */
    public String tryDecrypt(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return null;
        }
        try {
            return aes().decryptStr(cipherText);
        } catch (Exception ignored) {
            return null;
        }
    }

    public String maskSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            return null;
        }
        if (secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }

    public boolean isMaskedValue(String value) {
        return StringUtils.hasText(value) && value.contains("****");
    }

    private AES aes() {
        byte[] keyBytes = Arrays.copyOf(
                cryptoProperties.getCryptoKey().getBytes(StandardCharsets.UTF_8),
                32
        );
        return SecureUtil.aes(keyBytes);
    }
}

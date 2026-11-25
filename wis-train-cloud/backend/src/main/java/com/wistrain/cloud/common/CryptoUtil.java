package com.wistrain.cloud.common;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加密工具（NF-07）：智能体 API Key 加密落库，密钥走环境变量
 */
public class CryptoUtil {

    private CryptoUtil() {}

    private static final String ALGO = "AES/ECB/PKCS5Padding";

    public static String encrypt(String plain, String key16) {
        try {
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key16.getBytes(StandardCharsets.UTF_8), "AES"));
            return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BizException("密钥加密失败");
        }
    }

    public static String decrypt(String cipherBase64, String key16) {
        try {
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key16.getBytes(StandardCharsets.UTF_8), "AES"));
            return new String(cipher.doFinal(Base64.getDecoder().decode(cipherBase64)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BizException("密钥解密失败");
        }
    }

    /** 列表回显掩码：仅保留前 4 后 4 位 */
    public static String mask(String plain) {
        if (plain == null || plain.length() <= 8) {
            return "****";
        }
        return plain.substring(0, 4) + "****" + plain.substring(plain.length() - 4);
    }
}

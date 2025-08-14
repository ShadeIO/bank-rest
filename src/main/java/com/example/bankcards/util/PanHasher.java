package com.example.bankcards.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PanHasher {
    // секретный «перец» из настроек; можно reuse encrypt.secret, но лучше отдельный
    private static final String PEPPER = System.getProperty("pan.pepper",
            System.getenv().getOrDefault("PAN_PEPPER", "change-me-please"));

    public static String normalize(String pan) {
        if (pan == null) return null;
        return pan.replaceAll("\\s+", "");
    }

    public static String hmacSha256Base64(String normalizedPan) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(PEPPER.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(normalizedPan.getBytes(StandardCharsets.UTF_8));
            // можно хранить в hex, но base64 короче
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("PAN hash failed", e);
        }
    }
}

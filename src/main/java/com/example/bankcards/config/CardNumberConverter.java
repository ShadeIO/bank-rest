package com.example.bankcards.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
public class CardNumberConverter implements AttributeConverter<String, String> {

    @Value("${encrypt.secret}")
    private String KEY_B64;

    private byte[] KEY; // 32 bytes
    private static final String ALG = "AES";
    private static final String TR = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LEN = 128; // bits
    private static final int IV_LEN = 12; // bytes

    private static final SecureRandom RNG = new SecureRandom();

    @PostConstruct
    public void init() {
        if (KEY_B64 == null || KEY_B64.isBlank()) {
            throw new IllegalStateException("ENCRYPT_SECRET is not set — please configure encrypt.secret in application.yml or environment");
        }
        KEY = Base64.getDecoder().decode(KEY_B64);
        if (KEY.length != 32) {
            throw new IllegalStateException("ENCRYPT_SECRET must be a base64-encoded 256-bit key (32 bytes after decoding)");
        }
    }


    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return plaintext;
        try {
            byte[] iv = new byte[IV_LEN];
            RNG.nextBytes(iv);
            Cipher c = Cipher.getInstance(TR);
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, ALG), new GCMParameterSpec(GCM_TAG_LEN, iv));
            byte[] ct = c.doFinal(plaintext.getBytes());
            ByteBuffer bb = ByteBuffer.allocate(iv.length + ct.length);
            bb.put(iv).put(ct);
            return Base64.getEncoder().encodeToString(bb.array());
        } catch (Exception e) {
            throw new IllegalStateException("Encrypt failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return dbData;

        // 1) Если это не похоже на base64, значит это старый plain-text → вернём как есть
        if (!looksLikeBase64(dbData)) return dbData;

        // 2) Похоже на base64 — пробуем расшифровать. Если не вышло, считаем это legacy/plain и тоже вернём как есть.
        try {
            byte[] all = Base64.getDecoder().decode(dbData);
            if (all.length <= IV_LEN) return dbData;
            byte[] iv = new byte[IV_LEN];
            byte[] ct = new byte[all.length - IV_LEN];
            System.arraycopy(all, 0, iv, 0, IV_LEN);
            System.arraycopy(all, IV_LEN, ct, 0, ct.length);

            Cipher c = Cipher.getInstance(TR);
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, ALG), new GCMParameterSpec(GCM_TAG_LEN, iv));
            byte[] pt = c.doFinal(ct);
            return new String(pt);
        } catch (Exception ignore) {
            // старые записи или сторонний формат — не рвём приложение
            return dbData;
        }
    }

    private boolean looksLikeBase64(String s) {
        if (s.length() < 16 || (s.length() % 4 != 0)) return false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (!(ch >= 'A' && ch <= 'Z') &&
                    !(ch >= 'a' && ch <= 'z') &&
                    !(ch >= '0' && ch <= '9') &&
                    ch != '+' && ch != '/' && ch != '=')
                return false;
        }
        return true;
    }
}

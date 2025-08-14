package com.example.bankcards.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CardNumberConverterTest {

    static String base64Key;

    @BeforeAll
    static void key() {
        // генерируем 32-байтный ключ и кладём в системное свойство, как в конвертере
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        base64Key = Base64.getEncoder().encodeToString(key);
    }

    @Test
    void roundTrip_encryptsAndDecrypts() {
        CardNumberConverter c = new CardNumberConverter();
        ReflectionTestUtils.setField(c, "KEY_B64", base64Key);
        c.init();

        String pan = "5555555555554444";
        String db = c.convertToDatabaseColumn(pan);
        String back = c.convertToEntityAttribute(db);

        assertNotEquals(pan, db);                 // в БД должен быть не plaintext
        assertEquals(pan, back);
    }

    @Test
    void legacyPlaintext_passesThrough() {
        CardNumberConverter c = new CardNumberConverter();
        String old = "4111111111111111";          // "legacy" открытый номер
        String res = c.convertToEntityAttribute(old);
        assertEquals(old, res);                   // должен читаться без падения
    }
}

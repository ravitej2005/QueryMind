package com.querymind.connection.crypto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class CredentialCipherTest {

    // 32-byte test-only key, base64 encoded. Never a real secret.
    private final String testKey = Base64.getEncoder().encodeToString(new byte[32]);
    private final CredentialCipher cipher = new CredentialCipher(testKey);

    @Test
    void roundTripsPlaintext() {
        var encrypted = cipher.encrypt("super-secret-db-password");
        String decrypted = cipher.decrypt(encrypted.ciphertext(), encrypted.iv());
        assertEquals("super-secret-db-password", decrypted);
    }

    @Test
    void differentIvsForRepeatedEncryption() {
        var first = cipher.encrypt("same-password");
        var second = cipher.encrypt("same-password");
        assertFalse(java.util.Arrays.equals(first.iv(), second.iv()));
    }

    @Test
    void rejectsMissingKey() {
        assertThrows(IllegalStateException.class, () -> new CredentialCipher(""));
    }

    @Test
    void rejectsWrongLengthKey() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThrows(IllegalStateException.class, () -> new CredentialCipher(shortKey));
    }
}

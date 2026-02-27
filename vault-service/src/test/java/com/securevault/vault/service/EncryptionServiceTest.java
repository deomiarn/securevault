package com.securevault.vault.service;

import com.securevault.vault.model.EncryptedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

    private static final String TEST_MASTER_KEY = Base64.getEncoder()
            .encodeToString("this is a test key for dev only!".getBytes());

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService(TEST_MASTER_KEY);
    }

    @Test
    void encrypt_ReturnsNonNullResult() {
        EncryptedData result = encryptionService.encrypt("my-secret-password");

        assertThat(result).isNotNull();
        assertThat(result.encryptedValue()).isNotBlank();
        assertThat(result.iv()).isNotBlank();
    }

    @Test
    void encrypt_DifferentIvEachTime() {
        String plaintext = "same-plaintext";

        EncryptedData first = encryptionService.encrypt(plaintext);
        EncryptedData second = encryptionService.encrypt(plaintext);

        assertThat(first.iv()).isNotEqualTo(second.iv());
        assertThat(first.encryptedValue()).isNotEqualTo(second.encryptedValue());
    }

    @Test
    void decrypt_ReturnsOriginalPlaintext() {
        String original = "my-secret-password-123!@#";

        EncryptedData encrypted = encryptionService.encrypt(original);
        String decrypted = encryptionService.decrypt(encrypted.encryptedValue(), encrypted.iv());

        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void decrypt_WrongIv_ThrowsException() {
        EncryptedData encrypted = encryptionService.encrypt("test-value");
        // Generate a different IV by encrypting something else
        EncryptedData other = encryptionService.encrypt("other-value");

        assertThatThrownBy(() ->
                encryptionService.decrypt(encrypted.encryptedValue(), other.iv())
        ).isInstanceOf(RuntimeException.class);
    }

    @Test
    void decrypt_TamperedCiphertext_ThrowsException() {
        EncryptedData encrypted = encryptionService.encrypt("test-value");

        // Tamper with the ciphertext by modifying bytes
        byte[] ciphertextBytes = Base64.getDecoder().decode(encrypted.encryptedValue());
        ciphertextBytes[0] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(ciphertextBytes);

        assertThatThrownBy(() ->
                encryptionService.decrypt(tampered, encrypted.iv())
        ).isInstanceOf(RuntimeException.class);
    }

    @Test
    void encrypt_EmptyString_Works() {
        EncryptedData encrypted = encryptionService.encrypt("");
        String decrypted = encryptionService.decrypt(encrypted.encryptedValue(), encrypted.iv());

        assertThat(decrypted).isEmpty();
    }
}

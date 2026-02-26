package com.securevault.vault.model;

public record EncryptedData(
        String encryptedValue,
        String iv
) {
}

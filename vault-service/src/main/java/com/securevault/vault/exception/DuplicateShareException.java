package com.securevault.vault.exception;

public class DuplicateShareException extends RuntimeException {
    public DuplicateShareException(String message) {
        super(message);
    }
}

package com.securevault.auth.exception;


public class TotpVerificationException extends RuntimeException {
    public TotpVerificationException(String message) {
        super(message);
    }
}

package com.example.fingerprint_backend.custom_exception;

public class FingerprintProcessingException extends Exception {

    public FingerprintProcessingException(String message) {
        super(message);
    }

    public FingerprintProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
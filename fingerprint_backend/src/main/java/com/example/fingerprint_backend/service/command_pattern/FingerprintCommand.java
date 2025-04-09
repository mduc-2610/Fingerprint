package com.example.fingerprint_backend.service.command_pattern;

public interface FingerprintCommand<T> {
    T execute() throws Exception;
}


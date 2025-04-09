package com.example.fingerprint_backend.service.command_pattern;

import org.springframework.stereotype.Component;

@Component
public class FingerprintCommandExecutor {
    public <T> T runCommand(FingerprintCommand<T> command) throws Exception {
        return command.execute();
    }
}

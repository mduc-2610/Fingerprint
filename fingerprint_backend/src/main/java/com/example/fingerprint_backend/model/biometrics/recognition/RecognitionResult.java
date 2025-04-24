package com.example.fingerprint_backend.model.biometrics.recognition;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecognitionResult {
    private final String employeeId;
    private final double confidence;
    private final String fingerprintId;
    private final Boolean match;

    public String getEmployeeId() {
        return employeeId;
    }

    public double getConfidence() {
        return confidence;
    }
}
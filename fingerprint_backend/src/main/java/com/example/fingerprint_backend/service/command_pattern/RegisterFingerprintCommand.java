package com.example.fingerprint_backend.service.command_pattern;

import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.service.FingerprintService;
import org.springframework.web.multipart.MultipartFile;

public class RegisterFingerprintCommand implements FingerprintCommand<FingerprintSample> {

    private final FingerprintService fingerprintService;
    private final String employeeId;
    private final MultipartFile file;
    private final String position;
    private final String segmentationModelId;
    private final String recognitionModelId;

    public RegisterFingerprintCommand(FingerprintService fingerprintService,
                                      String employeeId,
                                      MultipartFile file,
                                      String position,
                                      String segmentationModelId,
                                      String recognitionModelId) {
        this.fingerprintService = fingerprintService;
        this.employeeId = employeeId;
        this.file = file;
        this.position = position;
        this.segmentationModelId = segmentationModelId;
        this.recognitionModelId = recognitionModelId;
    }

    @Override
    public FingerprintSample execute() throws Exception {
        return fingerprintService.registerFingerprint(employeeId, file, position, segmentationModelId, recognitionModelId);
    }
}

package com.example.fingerprint_backend.controller.biometrics.fingerprint;

import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.repository.access.AreaRepository;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
import com.example.fingerprint_backend.service.FingerprintService;
import com.example.fingerprint_backend.service.command_pattern.FingerprintCommandExecutor;
import com.example.fingerprint_backend.service.command_pattern.RecognizeFingerprintCommand;
import com.example.fingerprint_backend.service.command_pattern.RegisterFingerprintCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/fingerprint")
@RequiredArgsConstructor
    public class FingerprintController {
    private final FingerprintService fingerprintService;
    private final EmployeeRepository employeeRepository;
    private final AreaRepository areaRepository;
    private final FingerprintCommandExecutor commandExecutor;


    @PostMapping("/register/{employeeId}/single")
    public ResponseEntity<?> registerFingerprint(
            @PathVariable String employeeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("position") String position,
            @RequestParam(value = "segmentationModelId", required = false) String segmentationModelId,
            @RequestParam(value = "recognitionModelId", required = false) String recognitionModelId
    ) {
        try {
            RegisterFingerprintCommand command = new RegisterFingerprintCommand(
                    fingerprintService,
                    employeeId,
                    file,
                    position,
                    segmentationModelId,
                    recognitionModelId
            );

            FingerprintSample registeredSample = commandExecutor.runCommand(command);

            return ResponseEntity.ok(Map.of(
                    "message", "Fingerprint registered successfully",
                    "employeeId", employeeId,
                    "sample", registeredSample,
                    "segmentationModelId", segmentationModelId,
                    "recognitionModelId", recognitionModelId
            ));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/recognize")
    public ResponseEntity<?> recognizeFingerprint(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "segmentationModelId", required = false) String segmentationModelId,
            @RequestParam(value = "recognitionModelId", required = false) String recognitionModelId,
            @RequestParam(value = "areaId", required = false) String areaId,
            @RequestParam(value = "accessType", required = false, defaultValue = "ENTRY") String accessType) {
        try {
            RecognizeFingerprintCommand command = new RecognizeFingerprintCommand(
                    fingerprintService,
                    employeeRepository,
                    areaRepository,
                    file,
                    segmentationModelId,
                    recognitionModelId,
                    areaId,
                    accessType
            );

            Map<String, Object> result = commandExecutor.runCommand(command);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }


}

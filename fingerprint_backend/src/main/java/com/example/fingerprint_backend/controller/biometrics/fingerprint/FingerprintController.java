package com.example.fingerprint_backend.controller.biometrics.fingerprint;

import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.fingerprint_backend.model.biometrics.recognition.RecognitionResult;
import com.example.fingerprint_backend.service.FingerprintService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/fingerprint")
@RequiredArgsConstructor
public class FingerprintController {
    private final FingerprintService fingerprintService;
    private final EmployeeRepository employeeRepository;

    @PostMapping("/register/{employeeId}")
    public ResponseEntity<?> registerFingerprints(
            @PathVariable String employeeId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("segmentationModelId") String segmentationModelId,
            @RequestParam("recognitionModelId") String recognitionModelId
    ) {

        try {
            List<String> savedPaths = fingerprintService.registerFingerprints(employeeId, files, segmentationModelId, recognitionModelId);
            return ResponseEntity.ok(Map.of(
                    "message", "Fingerprints registered successfully",
                    "employeeId", employeeId,
                    "count", savedPaths.size(),
                    "paths", savedPaths
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
            @RequestParam("segmentationModelId") String segmentationModelId,
            @RequestParam("recognitionModelId") String recognitionModelId) {
        try {
            RecognitionResult result = fingerprintService.recognizeFingerprint(file, segmentationModelId, recognitionModelId);

            if (result == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Fingerprint recognition failed"));
            }

            if (result.isMatch()) {
                Optional<Employee> employee = employeeRepository.findById(result.getEmployeeId());

                if (employee.isEmpty()) {
                    return ResponseEntity.ok(Map.of(
                            "matched", true,
                            "employeeId", result.getEmployeeId(),
                            "confidence", result.getConfidence(),
                            "employee", "Employee not found"
                    ));
                }

                return ResponseEntity.ok(Map.of(
                        "matched", true,
                        "employeeId", result.getEmployeeId(),
                        "confidence", result.getConfidence(),
                        "employee", employee.get()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "matched", false,
                        "confidence", result.getConfidence()
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }
}
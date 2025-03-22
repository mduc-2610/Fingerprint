package com.example.fingerprint_backend.controller;

import com.example.fingerprint_backend.dto.EmployeeDTO;
import com.example.fingerprint_backend.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.fingerprint_backend.custom_exception.FingerprintProcessingException;
import com.example.fingerprint_backend.model.RecognitionResult;
import com.example.fingerprint_backend.service.FingerprintService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fingerprint")
public class FingerprintController {

    private final FingerprintService fingerprintService;
    private final EmployeeRepository employeeRepository;

    @Autowired
    public FingerprintController(FingerprintService fingerprintService, EmployeeRepository employeeRepository) {
        this.fingerprintService = fingerprintService;
        this.employeeRepository = employeeRepository;
    }

    @PostMapping("/register/{employeeId}")
    public ResponseEntity<?> registerFingerprints(
            @PathVariable String employeeId,
            @RequestParam("files") List<MultipartFile> files) {

        try {
            List<String> savedPaths = fingerprintService.registerFingerprints(employeeId, files);
            return ResponseEntity.ok(Map.of(
                    "message", "Fingerprints registered successfully",
                    "employeeId", employeeId,
                    "count", savedPaths.size(),
                    "paths", savedPaths
            ));
        } catch (FingerprintProcessingException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/recognize")
    public ResponseEntity<?> recognizeFingerprint(@RequestParam("file") MultipartFile file) {
        try {
            RecognitionResult result = fingerprintService.recognizeFingerprint(file);

            if (result == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Fingerprint recognition failed"));
            }

            if (result.isMatch()) {
                EmployeeDTO employee = employeeRepository.findByIdDTO(result.getEmployeeId());

                if (employee == null) {
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
                        "employee", employee
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "matched", false,
                        "confidence", result.getConfidence()
                ));
            }
        } catch (FingerprintProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

}

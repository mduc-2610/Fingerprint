package com.example.fingerprint_backend.controller.biometrics.fingerprint;

import com.example.fingerprint_backend.model.access.AccessLog;
import com.example.fingerprint_backend.model.access.Area;
import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.repository.access.AreaRepository;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSampleRepository;
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
    private final FingerprintSampleRepository fingerprintSampleRepository;
    private final AreaRepository areaRepository;

    @PostMapping("/register/{employeeId}/single")
    public ResponseEntity<?> registerFingerprint(
            @PathVariable String employeeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("position") String position,
            @RequestParam("segmentationModelId") String segmentationModelId,
            @RequestParam("recognitionModelId") String recognitionModelId
    ) {
        try {
            FingerprintSample registeredSample = fingerprintService.registerFingerprint(
                    employeeId, file, position, segmentationModelId, recognitionModelId);

            return ResponseEntity.ok(Map.of(
                    "message", "Fingerprint registered successfully",
                    "employeeId", employeeId,
                    "sample", registeredSample
            ));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

//    @PostMapping("/register/{employeeId}/multi")
//    public ResponseEntity<?> registerFingerprints(
//            @PathVariable String employeeId,
//            @RequestParam("files") List<MultipartFile> files,
//            @RequestParam("positions") List<String> positions,
//            @RequestParam("segmentationModelId") String segmentationModelId,
//            @RequestParam("recognitionModelId") String recognitionModelId
//    ) {
//        if (files.size() != positions.size()) {
//            return ResponseEntity
//                    .status(HttpStatus.BAD_REQUEST)
//                    .body(Map.of("error", "Number of files and positions must match"));
//        }
//
//        try {
//            List<FingerprintSample> registeredSamples = fingerprintService.registerFingerprints(
//                    employeeId, files, positions, segmentationModelId, recognitionModelId);
//
//            return ResponseEntity.ok(Map.of(
//                    "message", "Fingerprints registered successfully",
//                    "employeeId", employeeId,
//                    "count", registeredSamples.size(),
//                    "samples", registeredSamples
//            ));
//        } catch (Exception e) {
//            return ResponseEntity
//                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", e.getMessage()));
//        }
//    }

    @PostMapping("/recognize")
    public ResponseEntity<?> recognizeFingerprint(
            @RequestParam("file") MultipartFile file,
            @RequestParam("segmentationModelId") String segmentationModelId,
            @RequestParam("recognitionModelId") String recognitionModelId,
            @RequestParam(value = "areaId", required = false) String areaId,
            @RequestParam(value = "accessType", required = false, defaultValue = "ENTRY") String accessType) {
        try {
            Area area = null;
            if (areaId != null && !areaId.isEmpty()) {
                Optional<Area> areaOpt = areaRepository.findById(areaId);
                if (areaOpt.isPresent()) {
                    area = areaOpt.get();
                }
            }

            RecognitionResult result = fingerprintService.recognizeFingerprint(file, segmentationModelId, recognitionModelId);

            if (result == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Fingerprint recognition failed"));
            }

            // Create access log regardless of match result
            AccessLog accessLog = fingerprintService.createAccessLog(
                    result.getEmployeeId(),
                    area,
                    accessType,
                    result.isMatch(),
                    result.getConfidence(),
                    segmentationModelId,
                    recognitionModelId);

            if (result.isMatch()) {
                Optional<Employee> employee = employeeRepository.findById(result.getEmployeeId());

                if (employee.isEmpty()) {
                    return ResponseEntity.ok(Map.of(
                            "matched", true,
                            "employeeId", result.getEmployeeId(),
                            "confidence", result.getConfidence(),
                            "accessLog", accessLog,
                            "isAuthorized", accessLog.isAuthorized(),
                            "message", "Employee not found in the database but fingerprint matched"
                    ));
                }

                return ResponseEntity.ok(Map.of(
                        "matched", true,
                        "employeeId", result.getEmployeeId(),
                        "confidence", result.getConfidence(),
                        "employee", employee.get(),
                        "accessLog", accessLog,
                        "isAuthorized", accessLog.isAuthorized()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "matched", false,
                        "confidence", result.getConfidence(),
                        "accessLog", accessLog,
                        "isAuthorized", false
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/samples/{employeeId}")
    public ResponseEntity<?> getEmployeeFingerprints(@PathVariable String employeeId) {
        try {
            List<FingerprintSample> samples = fingerprintSampleRepository.findByEmployeeId(employeeId);

            if (samples.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No fingerprint samples found for employee " + employeeId));
            }

            return ResponseEntity.ok(samples);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

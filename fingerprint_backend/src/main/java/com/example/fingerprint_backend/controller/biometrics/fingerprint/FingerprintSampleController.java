package com.example.fingerprint_backend.controller.biometrics.fingerprint;

import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSampleRepository;
import com.example.fingerprint_backend.service.FingerprintRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/fingerprint-sample")
public class FingerprintSampleController {

    @Autowired
    private FingerprintSampleRepository fingerprintSampleRepository;

    @Autowired
    private FingerprintRegistrationService registrationService;

    @GetMapping
    public List<FingerprintSample> getAllFingerprintSamples() {
        return fingerprintSampleRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FingerprintSample> getFingerprintSampleById(@PathVariable String id) {
        Optional<FingerprintSample> fingerprintSample = fingerprintSampleRepository.findById(id);
        return fingerprintSample.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{employeeId}")
    public List<FingerprintSample> getFingerprintSamplesByEmployeeId(@PathVariable String employeeId) {
        return fingerprintSampleRepository.findByEmployeeId(employeeId);
    }

    @PostMapping
    public FingerprintSample createFingerprintSample(@RequestBody FingerprintSample fingerprintSample) {
        return fingerprintSampleRepository.save(fingerprintSample);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FingerprintSample> updateFingerprintSample(@PathVariable String id, @RequestBody FingerprintSample updatedSample) {
        return fingerprintSampleRepository.findById(id).map(existingSample -> {
            // Update only relevant fields
            // existingSample.setEmployeeId(updatedSample.getEmployeeId());
            // existingSample.setFingerprintData(updatedSample.getFingerprintData());
            FingerprintSample savedSample = fingerprintSampleRepository.save(existingSample);
            return ResponseEntity.ok(savedSample);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteFingerprintSample(@PathVariable String id) {
        return fingerprintSampleRepository.findById(id).map(existingSample -> {
            fingerprintSampleRepository.delete(existingSample);
            return ResponseEntity.noContent().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-recognition-model/{modelId}")
    public ResponseEntity<List<FingerprintSample>> getFingerprintsByRecognitionModel(@PathVariable String modelId) {
        List<FingerprintSample> samples = fingerprintSampleRepository.findByFingerprintRecognitionModelId(modelId);
        return ResponseEntity.ok(samples);
    }

    @GetMapping("/by-segmentation-model/{modelId}")
    public ResponseEntity<List<FingerprintSample>> getFingerprintsBySegmentationModel(@PathVariable String modelId) {
        List<FingerprintSample> samples = fingerprintSampleRepository.findByFingerprintSegmentationModelId(modelId);
        return ResponseEntity.ok(samples);
    }

    // Added registration endpoints from the previous FingerprintController

    @PostMapping("/register/{employeeId}/single")
    public ResponseEntity<?> registerFingerprint(
            @PathVariable String employeeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("position") String position,
            @RequestParam("segmentationModelId") String segmentationModelId,
            @RequestParam("recognitionModelId") String recognitionModelId
    ) {
        try {
            FingerprintSample registeredSample = registrationService.registerFingerprint(
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

    @PostMapping("/register/{employeeId}/multiple")
    public ResponseEntity<?> registerMultipleFingerprints(
            @PathVariable String employeeId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("positions") List<String> positions,
            @RequestParam("segmentationModelId") String segmentationModelId,
            @RequestParam("recognitionModelId") String recognitionModelId
    ) {
        try {
            if (files.size() != positions.size()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Number of files must match number of positions"));
            }

            List<FingerprintSample> registeredSamples = registrationService.registerFingerprints(
                    employeeId, files, positions, segmentationModelId, recognitionModelId);

            return ResponseEntity.ok(Map.of(
                    "message", "Fingerprints registered successfully",
                    "employeeId", employeeId,
                    "sampleCount", registeredSamples.size(),
                    "samples", registeredSamples
            ));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
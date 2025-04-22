package com.example.fingerprint_backend.controller.biometrics.fingerprint;

import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSampleRepository;
import com.example.fingerprint_backend.service.FingerprintRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
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

    @Autowired
    private EmployeeRepository employeeRepository;


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

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<FingerprintSample>> getEmployeeFingerprints(
            @PathVariable String employeeId,
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly) {

        // Check if employee exists
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Employee not found with id: " + employeeId);
        }

        List<FingerprintSample> samples;

        if (activeOnly) {
            samples = fingerprintSampleRepository.findByEmployeeIdAndActiveTrue(employeeId);
        } else {
            samples = fingerprintSampleRepository.findByEmployeeId(employeeId);
        }

        return ResponseEntity.ok(samples);
    }

    @PutMapping("/disable/{fingerprintId}")
    public ResponseEntity<Map<String, Object>> disableFingerprint(@PathVariable String fingerprintId) {
        // Check if fingerprint exists
        Optional<FingerprintSample> fingerprintOptional = fingerprintSampleRepository.findById(fingerprintId);

        if (fingerprintOptional.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Fingerprint sample not found with id: " + fingerprintId);
        }

        FingerprintSample fingerprint = fingerprintOptional.get();

        // Disable fingerprint by setting active to false
        fingerprint.setActive(false);
        fingerprintSampleRepository.save(fingerprint);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Fingerprint disabled successfully");
        response.put("fingerprintId", fingerprintId);
        response.put("employeeId", fingerprint.getEmployee().getId());
        response.put("employeeName", fingerprint.getEmployee().getFullName());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/enable/{fingerprintId}")
    public ResponseEntity<Map<String, Object>> enableFingerprint(@PathVariable String fingerprintId) {
        // Check if fingerprint exists
        Optional<FingerprintSample> fingerprintOptional = fingerprintSampleRepository.findById(fingerprintId);

        if (fingerprintOptional.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Fingerprint sample not found with id: " + fingerprintId);
        }

        FingerprintSample fingerprint = fingerprintOptional.get();
        Employee employee = fingerprint.getEmployee();

        // Check if enabling would exceed the maximum number of samples
        if (!fingerprint.isActive()) {
            int activeFingerprints = fingerprintSampleRepository.countByEmployeeIdAndActiveTrue(employee.getId());

            if (activeFingerprints >= employee.getMaxNumberSamples()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot enable fingerprint. Employee has reached the maximum number of samples: " + employee.getMaxNumberSamples());
            }
        }

        // Enable fingerprint by setting active to true
        fingerprint.setActive(true);
        fingerprintSampleRepository.save(fingerprint);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Fingerprint enabled successfully");
        response.put("fingerprintId", fingerprintId);
        response.put("employeeId", employee.getId());
        response.put("employeeName", employee.getFullName());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/disable-all/{employeeId}")
    public ResponseEntity<Map<String, Object>> disableAllFingerprints(@PathVariable String employeeId) {
        // Check if employee exists
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Employee not found with id: " + employeeId));

        // Disable all fingerprints for the employee
        int updatedCount = fingerprintSampleRepository.deactivateAllForEmployee(employeeId);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "All fingerprints disabled successfully");
        response.put("employeeId", employeeId);
//        response.put("employeeName", employee.getFullName());
        response.put("disabledCount", updatedCount);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{fingerprintId}")
    public ResponseEntity<Map<String, Object>> deleteFingerprint(@PathVariable String fingerprintId) {
        // Check if fingerprint exists
        Optional<FingerprintSample> fingerprintOptional = fingerprintSampleRepository.findById(fingerprintId);

        if (fingerprintOptional.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Fingerprint sample not found with id: " + fingerprintId);
        }

        FingerprintSample fingerprint = fingerprintOptional.get();
        String employeeId = fingerprint.getEmployee().getId();
        String employeeName = fingerprint.getEmployee().getFullName();

        // Delete fingerprint
        fingerprintSampleRepository.deleteById(fingerprintId);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Fingerprint deleted successfully");
        response.put("fingerprintId", fingerprintId);
        response.put("employeeId", employeeId);
        response.put("employeeName", employeeName);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/set-max-samples/{employeeId}")
    public ResponseEntity<Map<String, Object>> setMaxSamples(
            @PathVariable String employeeId,
            @RequestParam int maxSamples) {

        if (maxSamples < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Maximum samples must be at least 1");
        }

        // Check if employee exists
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Employee not found with id: " + employeeId));

        // Update max samples
        employee.setMaxNumberSamples(maxSamples);
        employeeRepository.save(employee);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Maximum fingerprint samples updated successfully");
        response.put("employeeId", employeeId);
        response.put("employeeName", employee.getFullName());
        response.put("maxSamples", maxSamples);

        return ResponseEntity.ok(response);
    }
}
package com.example.fingerprint_backend.controller.biometrics.fingerprint;

import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSampleRepository;
import com.example.fingerprint_backend.service.FingerprintManagementService;
import com.example.fingerprint_backend.service.FingerprintRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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

    @Autowired
    private FingerprintManagementService fingerprintManagementService;


    @GetMapping
    public List<FingerprintSample> getAllFingerprintSamples() {
        return fingerprintSampleRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FingerprintSample> getFingerprintSampleById(@PathVariable String id) {
        Optional<FingerprintSample> fingerprintSample = fingerprintSampleRepository.findById(id);
        return fingerprintSample.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
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

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<FingerprintSample>> getFingerprintSamplesByEmployeeId(
            @PathVariable String employeeId,
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly) {

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
    public ResponseEntity<Map<String, Object>> disableFingerprint(@PathVariable String fingerprintId) throws IOException {
        Optional<FingerprintSample> fingerprintOptional = fingerprintSampleRepository.findById(fingerprintId);

        if (fingerprintOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fingerprint sample not found with id: " + fingerprintId);
        }

        FingerprintSample fingerprint = fingerprintOptional.get();
        String employeeId = fingerprint.getEmployee().getId();
        try {
            fingerprintManagementService.moveFileToDisabledDirectory(fingerprint.getImage(), employeeId);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Error while moving files to disabled directory: " + e.getMessage());
        }

        fingerprint.setActive(false);
        fingerprintSampleRepository.save(fingerprint);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Fingerprint disabled successfully");
        response.put("fingerprintId", fingerprintId);
        response.put("employeeId", employeeId);
        response.put("employeeName", fingerprint.getEmployee().getFullName());

        return ResponseEntity.ok(response);
    }


    @PutMapping("/enable/{fingerprintId}")
    public ResponseEntity<Map<String, Object>> enableFingerprint(@PathVariable String fingerprintId) throws IOException {
        Optional<FingerprintSample> fingerprintOptional = fingerprintSampleRepository.findById(fingerprintId);

        if (fingerprintOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fingerprint sample not found with id: " + fingerprintId);
        }

        FingerprintSample fingerprint = fingerprintOptional.get();
        Employee employee = fingerprint.getEmployee();
        String employeeId = employee.getId();

        try {
            fingerprintManagementService.moveFileToAdaptingDirectory(fingerprint.getImage(), employeeId);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Error while moving files to disabled directory: " + e.getMessage());
        }

        fingerprint.setActive(true);
        fingerprintSampleRepository.save(fingerprint);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Fingerprint enabled successfully");
        response.put("fingerprintId", fingerprintId);
        response.put("employeeId", employeeId);
        response.put("employeeName", employee.getFullName());

        return ResponseEntity.ok(response);
    }


    @PutMapping("/disable-all/{employeeId}")
    public ResponseEntity<Map<String, Object>> disableAllFingerprints(@PathVariable String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Employee not found with id: " + employeeId));

        List<FingerprintSample> activeSamples = fingerprintSampleRepository.findByEmployeeIdAndActiveTrue(employeeId);

        try {
            fingerprintManagementService.moveAllActiveFilesToDisabledDirectory(activeSamples, employeeId);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Error while moving files to disabled directory: " + e.getMessage());
        }

        int updatedCount = fingerprintSampleRepository.deactivateAllForEmployee(employeeId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "All fingerprints disabled successfully");
        response.put("employeeId", employeeId);
        response.put("disabledCount", updatedCount);

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{fingerprintId}")
    public ResponseEntity<Map<String, Object>> deleteFingerprintSample(@PathVariable String fingerprintId) {
        Optional<FingerprintSample> fingerprintOptional = fingerprintSampleRepository.findById(fingerprintId);

        if (fingerprintOptional.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Fingerprint sample not found with id: " + fingerprintId);
        }

        FingerprintSample fingerprint = fingerprintOptional.get();
        String employeeId = fingerprint.getEmployee().getId();
        String employeeName = fingerprint.getEmployee().getFullName();

        try {
            fingerprintManagementService.deleteFingerprintImage(fingerprint.getImage(), employeeId);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete fingerprint image file: " + e.getMessage());
        }

        fingerprintSampleRepository.deleteById(fingerprintId);

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

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Employee not found with id: " + employeeId));

        employee.setMaxNumberSamples(maxSamples);
        employeeRepository.save(employee);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Maximum fingerprint samples updated successfully");
        response.put("employeeId", employeeId);
        response.put("employeeName", employee.getFullName());
        response.put("maxSamples", maxSamples);

        return ResponseEntity.ok(response);
    }
}
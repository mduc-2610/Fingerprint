package com.example.fingerprint_backend.controller.biometrics.fingerprint;

import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/fingerprint-sample")
public class FingerprintSampleController {

    @Autowired
    private FingerprintSampleRepository fingerprintSampleRepository;

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
//            existingSample.setEmployeeId(updatedSample.getEmployeeId());
//            existingSample.setFingerprintData(updatedSample.getFingerprintData());
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
}
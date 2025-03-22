package com.example.fingerprint_backend.controller;

import com.example.fingerprint_backend.model.Recognition;
import com.example.fingerprint_backend.repository.RecognitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/recognitions")
@RequiredArgsConstructor
public class RecognitionController {

    private final RecognitionRepository recognitionRepository;

    @GetMapping
    public List<Recognition> getAllRecognitions() {
        return recognitionRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recognition> getRecognitionById(@PathVariable String id) {
        Optional<Recognition> recognition = recognitionRepository.findById(id);
        return recognition.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{employeeId}")
    public List<Recognition> getRecognitionsByEmployee(@PathVariable String employeeId) {
        return recognitionRepository.findByEmployeeId(employeeId);
    }

    @GetMapping("/confidence/{threshold}")
    public List<Recognition> getRecognitionsByConfidence(@PathVariable float threshold) {
        return recognitionRepository.findByConfidenceGreaterThan(threshold);
    }

    @PostMapping
    public Recognition createRecognition(@RequestBody Recognition recognition) {
        return recognitionRepository.save(recognition);
    }
}

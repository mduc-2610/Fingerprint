package com.example.fingerprint_backend.controller.base;

import com.example.fingerprint_backend.model.base.TrainingData;
import com.example.fingerprint_backend.repository.base.TrainingDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/training-data")
public class TrainingDataController {

    @Autowired
    private TrainingDataRepository trainingDataRepository;

    @GetMapping
    public List<TrainingData> getAllTrainingData() {
        return trainingDataRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainingData> getTrainingDataById(@PathVariable String id) {
        Optional<TrainingData> trainingData = trainingDataRepository.findById(id);
        return trainingData.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public TrainingData createTrainingData(@RequestBody TrainingData trainingData) {
        return trainingDataRepository.save(trainingData);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrainingData> updateTrainingData(@PathVariable String id, @RequestBody TrainingData updatedData) {
        return trainingDataRepository.findById(id).map(existingData -> {
            existingData.setName(updatedData.getName());
            existingData.setPurpose(updatedData.getPurpose());
            existingData.setSource(updatedData.getSource());
            existingData.setFormat(updatedData.getFormat());
            TrainingData savedData = trainingDataRepository.save(existingData);
            return ResponseEntity.ok(savedData);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteTrainingData(@PathVariable String id) {
        return trainingDataRepository.findById(id).map(existingData -> {
            trainingDataRepository.delete(existingData);
            return ResponseEntity.noContent().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}


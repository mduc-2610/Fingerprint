package com.example.fingerprint_backend.repository.biometrics.recognition;

import com.example.fingerprint_backend.model.biometrics.recognition.Recognition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecognitionRepository extends JpaRepository<Recognition, String> {
    List<Recognition> findByEmployeeId(String employeeId);
    List<Recognition> findByConfidenceGreaterThan(float confidenceThreshold);
}


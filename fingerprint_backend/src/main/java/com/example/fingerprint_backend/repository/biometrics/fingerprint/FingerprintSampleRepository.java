package com.example.fingerprint_backend.repository.biometrics.fingerprint;

import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSegmentationModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FingerprintSampleRepository extends JpaRepository<FingerprintSample, String> {
    List<FingerprintSample> findByEmployeeId(String employeeId);
    List<FingerprintSample> findByFingerprintSegmentationModelId(String modelId);
    List<FingerprintSample> findByFingerprintRecognitionModelId(String modelId);
    int countByFingerprintSegmentationModel(FingerprintSegmentationModel model);

}
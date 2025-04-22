package com.example.fingerprint_backend.repository.biometrics.fingerprint;

import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSegmentationModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FingerprintSampleRepository extends JpaRepository<FingerprintSample, String> {
    List<FingerprintSample> findByEmployeeId(String employeeId);
    List<FingerprintSample> findByFingerprintSegmentationModelId(String modelId);
    List<FingerprintSample> findByFingerprintRecognitionModelId(String modelId);
    int countByFingerprintSegmentationModel(FingerprintSegmentationModel model);

    List<FingerprintSample> findByEmployeeIdAndActiveTrue(String employeeId);

    int countByEmployeeIdAndActiveTrue(String employeeId);

    boolean existsByIdAndEmployeeId(String id, String employeeId);

    @Modifying
    @Transactional
    @Query("UPDATE FingerprintSample f SET f.active = :active WHERE f.id = :id")
    int updateActiveStatus(String id, boolean active);

    @Modifying
    @Transactional
    @Query("UPDATE FingerprintSample f SET f.active = false WHERE f.employee.id = :employeeId")
    int deactivateAllForEmployee(String employeeId);
}
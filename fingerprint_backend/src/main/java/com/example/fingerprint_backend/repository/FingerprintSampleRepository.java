package com.example.fingerprint_backend.repository;

import com.example.fingerprint_backend.model.FingerprintSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FingerprintSampleRepository extends JpaRepository<FingerprintSample, String> {
    List<FingerprintSample> findByEmployeeId(String employeeId);
}
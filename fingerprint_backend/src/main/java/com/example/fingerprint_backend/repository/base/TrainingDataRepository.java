package com.example.fingerprint_backend.repository.base;

import com.example.fingerprint_backend.model.base.TrainingData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingDataRepository extends JpaRepository<TrainingData, String> {
    List<TrainingData> findByPurpose(String purpose);

    List<TrainingData> findBySource(String source);

    List<TrainingData> findByNameContaining(String name);

    List<TrainingData> findByFormat(String format);
}

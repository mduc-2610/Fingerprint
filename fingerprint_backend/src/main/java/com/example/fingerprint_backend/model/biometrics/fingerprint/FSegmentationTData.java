package com.example.fingerprint_backend.model.biometrics.fingerprint;

import com.example.fingerprint_backend.model.base.TrainingData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FSegmentationTData {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "training_data_id")
    private TrainingData trainingData;

    @ManyToOne
    @JoinColumn(name = "fingerprint_region_model_id")
    private FingerprintSegmentationModel fingerprintSegmentationModel;
}
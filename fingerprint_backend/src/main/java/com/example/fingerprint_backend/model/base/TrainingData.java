package com.example.fingerprint_backend.model.base;

import com.example.fingerprint_backend.model.biometrics.fingerprint.FRecognitionTData;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FSegmentationTData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingData {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;
    private String purpose;
    private int sampleCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String source;
    private String resolution;
    private String format;

    @OneToMany(mappedBy = "trainingData")
    private List<FSegmentationTData> regionTData;

    @OneToMany(mappedBy = "trainingData")
    private List<FRecognitionTData> recognitionTData;
}
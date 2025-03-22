package com.example.fingerprint_backend.model;

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
    private String id;
    private String name;
    private String purpose;
    private int sampleCount;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "trainingData")
    private List<FRegionTData> regionTData;

    @OneToMany(mappedBy = "trainingData")
    private List<FRecognitionTData> recognitionTData;
}

package com.example.fingerprint_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FingerprintSample {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    // Path to the fingerprint image
    private String image;

    // Store the actual image data (optional - can be large)
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] imageData;

    // Position of the fingerprint (e.g., "right_thumb", "left_index")
    private String position;

    // When the fingerprint was captured
    private LocalDateTime capturedAt;

    // Added quality score
    private Double quality;

    // Added recognition confidence when this sample was last used
    private Double lastRecognitionConfidence;

    // Added path within the file system
    private String relativePath;
}

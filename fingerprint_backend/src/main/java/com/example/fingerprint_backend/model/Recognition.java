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
public class Recognition {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "camera_image_id")
    private CameraImage cameraImage;

    @ManyToOne
    @JoinColumn(name = "fingerprint_recognition_model_id")
    private FingerprintRecognitionModel fingerprintRecognitionModel;

    @ManyToOne
    @JoinColumn(name = "fingerprint_region_model_id")
    private FingerprintRegionModel fingerprintRegionModel;

    @ManyToOne
    @JoinColumn(name = "access_log_id")
    private AccessLog accessLog;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private LocalDateTime timestamp;
    private float confidence;
}


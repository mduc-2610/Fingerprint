package com.example.fingerprint_backend.repository.access;

import com.example.fingerprint_backend.model.access.CameraImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CameraImageRepository extends JpaRepository<CameraImage, String> {
    List<CameraImage> findByCameraId(String cameraId);

    List<CameraImage> findByCapturedAtBetween(LocalDateTime start, LocalDateTime end);

    CameraImage findTopByCameraIdOrderByCapturedAtDesc(String cameraId);
}
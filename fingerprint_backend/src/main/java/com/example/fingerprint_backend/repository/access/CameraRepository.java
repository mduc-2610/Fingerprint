package com.example.fingerprint_backend.repository.access;

import com.example.fingerprint_backend.model.access.Camera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CameraRepository extends JpaRepository<Camera, String> {
    List<Camera> findByAreaId(String areaId);
}
package com.example.fingerprint_backend.controller.access;

import com.example.fingerprint_backend.model.access.Camera;
import com.example.fingerprint_backend.model.access.CameraImage;
import com.example.fingerprint_backend.repository.access.CameraRepository;
import com.example.fingerprint_backend.repository.access.CameraImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/camera")
public class CameraController {

    @Autowired
    private CameraRepository cameraRepository;

    @Autowired
    private CameraImageRepository cameraImageRepository;

    @GetMapping
    public List<Camera> getAllCameras() {
        return cameraRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Camera> getCameraById(@PathVariable String id) {
        Optional<Camera> camera = cameraRepository.findById(id);
        return camera.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Camera createCamera(@RequestBody Camera camera) {
        return cameraRepository.save(camera);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Camera> updateCamera(@PathVariable String id, @RequestBody Camera updatedCamera) {
        return cameraRepository.findById(id).map(existingCamera -> {
            existingCamera.setArea(updatedCamera.getArea());
//            existingCamera.setName(updatedCamera.getName());
            Camera savedCamera = cameraRepository.save(existingCamera);
            return ResponseEntity.ok(savedCamera);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteCamera(@PathVariable String id) {
        return cameraRepository.findById(id).map(existingCamera -> {
            cameraRepository.delete(existingCamera);
            return ResponseEntity.noContent().build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{cameraId}/images")
    public List<CameraImage> getImagesByCamera(@PathVariable String cameraId) {
        return cameraImageRepository.findByCameraId(cameraId);
    }

    @GetMapping("/{cameraId}/latest-image")
    public ResponseEntity<CameraImage> getLatestImageByCamera(@PathVariable String cameraId) {
        CameraImage latestImage = cameraImageRepository.findTopByCameraIdOrderByCapturedAtDesc(cameraId);
        return latestImage != null ? ResponseEntity.ok(latestImage) : ResponseEntity.notFound().build();
    }

    @GetMapping("/images")
    public List<CameraImage> getImagesBetween(@RequestParam LocalDateTime start, @RequestParam LocalDateTime end) {
        return cameraImageRepository.findByCapturedAtBetween(start, end);
    }
}


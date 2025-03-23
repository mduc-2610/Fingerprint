package com.example.fingerprint_backend.controller.access;

import com.example.fingerprint_backend.model.access.Area;
import com.example.fingerprint_backend.model.access.Camera;
import com.example.fingerprint_backend.repository.access.AreaRepository;
import com.example.fingerprint_backend.repository.access.CameraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/area")
@RequiredArgsConstructor
public class AreaController {

    private final AreaRepository areaRepository;
    private final CameraRepository cameraRepository;

    @GetMapping
    public List<Area> getAllAreas() {
        return areaRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Area> getAreaById(@PathVariable String id) {
        Optional<Area> area = areaRepository.findById(id);
        return area.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Area createArea(@RequestBody Area area) {
        return areaRepository.save(area);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Area> updateArea(@PathVariable String id, @RequestBody Area area) {
        if (!areaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        area.setId(id);
        return ResponseEntity.ok(areaRepository.save(area));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArea(@PathVariable String id) {
        if (!areaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        areaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/cameras")
    public ResponseEntity<List<Camera>> getAreaCameras(@PathVariable String id) {
        if (!areaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<Camera> cameras = cameraRepository.findByAreaId(id);
        return ResponseEntity.ok(cameras);
    }
}

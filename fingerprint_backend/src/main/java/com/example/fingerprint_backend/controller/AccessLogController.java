package com.example.fingerprint_backend.controller;

import com.example.fingerprint_backend.model.AccessLog;
import com.example.fingerprint_backend.repository.AccessLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/access-logs")
@RequiredArgsConstructor
public class AccessLogController {

    private final AccessLogRepository accessLogRepository;

    @GetMapping
    public List<AccessLog> getAllAccessLogs() {
        return accessLogRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccessLog> getAccessLogById(@PathVariable String id) {
        Optional<AccessLog> accessLog = accessLogRepository.findById(id);
        return accessLog.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{employeeId}")
    public List<AccessLog> getAccessLogsByEmployee(@PathVariable String employeeId) {
        return accessLogRepository.findByEmployeeId(employeeId);
    }

    @GetMapping("/area/{areaId}")
    public List<AccessLog> getAccessLogsByArea(@PathVariable String areaId) {
        return accessLogRepository.findByAreaId(areaId);
    }

    @GetMapping("/timeframe")
    public List<AccessLog> getAccessLogsByTimeframe(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return accessLogRepository.findByTimestampBetween(start, end);
    }

    @PostMapping
    public AccessLog createAccessLog(@RequestBody AccessLog accessLog) {
        return accessLogRepository.save(accessLog);
    }
}

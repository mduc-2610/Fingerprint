package com.example.fingerprint_backend.repository.access;


import com.example.fingerprint_backend.model.access.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, String> {
    List<AccessLog> findByEmployeeId(String employeeId);
    List<AccessLog> findByAreaId(String areaId);
    List<AccessLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
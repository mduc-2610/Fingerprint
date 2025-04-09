package com.example.fingerprint_backend.service.strategy_pattern;

import com.example.fingerprint_backend.model.access.AccessLog;
import com.example.fingerprint_backend.model.analytics.EmployeeStatistics;
import com.example.fingerprint_backend.model.auth.Employee;

import java.time.LocalDateTime;
import java.util.List;

public interface EmployeeStatisticsStrategy {
    List<EmployeeStatistics> calculateStatistics(List<Employee> employees, List<AccessLog> accessLogs,
                                                 LocalDateTime startDate, LocalDateTime endDate);
}

package com.example.fingerprint_backend.service.template_pattern;

import com.example.fingerprint_backend.model.access.AccessLog;
import com.example.fingerprint_backend.model.analytics.EmployeeStatistics;
import com.example.fingerprint_backend.model.auth.Employee;

import java.time.LocalDateTime;
import java.util.List;

public class AccessCountStatistics extends EmployeeStatisticsTemplate {

    @Override
    protected EmployeeStatistics calculateEmployeeStatistics(Employee employee, List<AccessLog> accessLogs) {
        LocalDateTime firstAccess = accessLogs.stream()
                .map(AccessLog::getTimestamp)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime lastAccess = accessLogs.stream()
                .map(AccessLog::getTimestamp)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new EmployeeStatistics(
                employee.getId(),
                employee.getFullName(),
                accessLogs.size(),  // count of access logs
                firstAccess,
                lastAccess
        );
    }
}

package com.example.fingerprint_backend.service.template_pattern;

import com.example.fingerprint_backend.model.access.AccessLog;
import com.example.fingerprint_backend.model.analytics.EmployeeStatistics;
import com.example.fingerprint_backend.model.auth.Employee;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class AreaBasedStatistics extends EmployeeStatisticsTemplate {

    private final String areaId;

    public AreaBasedStatistics(String areaId) {
        this.areaId = areaId;
    }

    @Override
    protected List<AccessLog> filterAccessLogs(List<AccessLog> accessLogs, LocalDateTime startDate, LocalDateTime endDate) {
        return accessLogs.stream()
                .filter(log -> (startDate == null || !log.getTimestamp().isBefore(startDate))
                        && (endDate == null || !log.getTimestamp().isAfter(endDate))
                        && (areaId == null || log.getArea().getId().equals(areaId)))
                .collect(Collectors.toList());
    }

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

        // Count access logs per area
        int accessCount = accessLogs.size();

        return new EmployeeStatistics(
                employee.getId(),
                employee.getFullName(),
                accessCount,
                firstAccess,
                lastAccess
        );
    }
}

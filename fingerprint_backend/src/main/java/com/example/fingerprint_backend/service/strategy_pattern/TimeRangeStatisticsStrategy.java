package com.example.fingerprint_backend.service.strategy_pattern;

import com.example.fingerprint_backend.model.analytics.EmployeeStatistics;
import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.model.access.AccessLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class TimeRangeStatisticsStrategy implements EmployeeStatisticsStrategy {
    @Override
    public List<EmployeeStatistics> calculateStatistics(List<Employee> employees, List<AccessLog> accessLogs,
                                                        LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, List<AccessLog>> accessLogsByEmployee = accessLogs.stream()
                .filter(log -> (startDate == null || log.getTimestamp().isAfter(startDate) || log.getTimestamp().isEqual(startDate))
                        && (endDate == null || log.getTimestamp().isBefore(endDate) || log.getTimestamp().isEqual(endDate)))
                .collect(Collectors.groupingBy(log -> log.getEmployee().getId()));

        return employees.stream()
                .map(employee -> {
                    List<AccessLog> employeeLogs = accessLogsByEmployee.getOrDefault(employee.getId(), Collections.emptyList());
                    LocalDateTime firstAccess = employeeLogs.stream()
                            .map(AccessLog::getTimestamp)
                            .min(LocalDateTime::compareTo)
                            .orElse(null);
                    LocalDateTime lastAccess = employeeLogs.stream()
                            .map(AccessLog::getTimestamp)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);

                    return new EmployeeStatistics(
                            employee.getId(),
                            employee.getFullName(),
                            employeeLogs.size(),
                            firstAccess,
                            lastAccess
                    );
                })
                .collect(Collectors.toList());
    }
}
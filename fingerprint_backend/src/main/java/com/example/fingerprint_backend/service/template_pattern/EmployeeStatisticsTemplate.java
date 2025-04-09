package com.example.fingerprint_backend.service.template_pattern;

import com.example.fingerprint_backend.model.access.AccessLog;
import com.example.fingerprint_backend.model.analytics.EmployeeStatistics;
import com.example.fingerprint_backend.model.auth.Employee;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class EmployeeStatisticsTemplate {
    public final List<EmployeeStatistics> generateStatistics(
            List<Employee> employees,
            List<AccessLog> accessLogs,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        List<AccessLog> filteredLogs = filterAccessLogs(accessLogs, startDate, endDate);

        Map<String, List<AccessLog>> groupedLogs = groupLogsByEmployee(filteredLogs);

        return employees.stream()
                .map(employee -> calculateEmployeeStatistics(employee, groupedLogs.getOrDefault(employee.getId(), List.of())))
                .collect(Collectors.toList());
    }

    protected List<AccessLog> filterAccessLogs(List<AccessLog> accessLogs, LocalDateTime startDate, LocalDateTime endDate) {
        return accessLogs.stream()
                .filter(log -> (startDate == null || !log.getTimestamp().isBefore(startDate))
                        && (endDate == null || !log.getTimestamp().isAfter(endDate)))
                .collect(Collectors.toList());
    }

    protected Map<String, List<AccessLog>> groupLogsByEmployee(List<AccessLog> accessLogs) {
        return accessLogs.stream()
                .filter(log -> log.getEmployee() != null)
                .collect(Collectors.groupingBy(log -> log.getEmployee().getId()));
    }

    protected abstract EmployeeStatistics calculateEmployeeStatistics(Employee employee, List<AccessLog> accessLogs);
}

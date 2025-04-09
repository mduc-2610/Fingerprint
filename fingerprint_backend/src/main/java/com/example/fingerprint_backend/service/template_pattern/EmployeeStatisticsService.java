package com.example.fingerprint_backend.service.template_pattern;

import com.example.fingerprint_backend.model.access.AccessLog;
import com.example.fingerprint_backend.model.analytics.EmployeeStatistics;
import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.repository.access.AccessLogRepository;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeStatisticsService {

    private final EmployeeRepository employeeRepository;
    private final AccessLogRepository accessLogRepository;

    public List<EmployeeStatistics> getStatistics(
            EmployeeStatisticsTemplate template,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        List<Employee> employees = employeeRepository.findAll();
        List<AccessLog> accessLogs = accessLogRepository.findByTimestampBetween(
                startDate != null ? startDate : LocalDateTime.MIN,
                endDate != null ? endDate : LocalDateTime.now()
        );

        return template.generateStatistics(employees, accessLogs, startDate, endDate);
    }
}

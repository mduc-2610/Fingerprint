package com.example.fingerprint_backend.model.analytics;

import com.example.fingerprint_backend.model.auth.Employee;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeStatistics {
    @Id
    private String id;

    @OneToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private int totalEntries;
    private int totalExits;
    private int failAttempts;
    private int successAttempts;
}
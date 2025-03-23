package com.example.fingerprint_backend.model.access;

import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.model.biometrics.recognition.Recognition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "area_id")
    private Area area;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private LocalDateTime timestamp;
    private boolean isAuthorized;
    private String accessType;

    @OneToMany(mappedBy = "accessLog")
    private List<Recognition> recognitions;
}
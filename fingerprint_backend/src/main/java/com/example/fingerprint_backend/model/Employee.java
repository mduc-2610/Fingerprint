package com.example.fingerprint_backend.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class Employee extends User {
    // No need to redeclare id, fullName, etc. since they're inherited from User


    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<AccessLog> accessLogs;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<Recognition> recognitions;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<FingerprintSample> fingerprintSamples;

}
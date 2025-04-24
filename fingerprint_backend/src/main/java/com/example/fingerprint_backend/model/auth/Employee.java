package com.example.fingerprint_backend.model.auth;

import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.model.biometrics.recognition.Recognition;
import com.example.fingerprint_backend.model.access.AccessLog;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.*;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Data
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
public class Employee {

    @Id
    private String id;
    private String fullName;
    private String phoneNumber;
    private String photo;
    private String address;
    private int maxNumberSamples;

    public Employee(String id, String fullName, String phoneNumber, String photo, String address, int maxNumberSamples) {
        this.id = id;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.photo = photo;
        this.address = address;
        this.maxNumberSamples = maxNumberSamples;
    }

    @JsonIgnore
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AccessLog> accessLogs;

    @JsonIgnore
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Recognition> recognitions;

    @JsonIgnore
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FingerprintSample> fingerprintSamples;
}
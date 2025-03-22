package com.example.fingerprint_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Model {
    @Id
    private String id;
    private String name;
    private float accuracy;
    private float validationScores;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


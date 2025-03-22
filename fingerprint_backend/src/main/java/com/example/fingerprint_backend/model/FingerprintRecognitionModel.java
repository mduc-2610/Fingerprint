package com.example.fingerprint_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FingerprintRecognitionModel extends Model {

    @OneToMany(mappedBy = "fingerprintRecognitionModel")
    private List<Recognition> recognitions;

    @OneToMany(mappedBy = "fingerprintRecognitionModel")
    private List<FRecognitionTData> recognitionTData;
}

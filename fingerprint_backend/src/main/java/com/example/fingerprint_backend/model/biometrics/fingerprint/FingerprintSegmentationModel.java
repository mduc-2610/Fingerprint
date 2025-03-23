package com.example.fingerprint_backend.model.biometrics.fingerprint;

import com.example.fingerprint_backend.model.base.Model;
import com.example.fingerprint_backend.model.biometrics.recognition.Recognition;
import jakarta.persistence.FetchType;
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
public class FingerprintSegmentationModel extends Model {

    @OneToMany(mappedBy = "fingerprintSegmentationModel", fetch = FetchType.LAZY)
    private List<Recognition> recognitions;

    @OneToMany(mappedBy = "fingerprintSegmentationModel", fetch = FetchType.LAZY)
    private List<FSegmentationTData> regionTData;
}

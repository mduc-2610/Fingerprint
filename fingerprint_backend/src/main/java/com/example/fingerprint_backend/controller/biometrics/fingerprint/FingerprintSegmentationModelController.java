package com.example.fingerprint_backend.controller.biometrics.fingerprint;

import com.example.fingerprint_backend.controller.base.ModelController;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSegmentationModel;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSegmentationModelRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fingerprint-segmentation")
public class FingerprintSegmentationModelController
        extends ModelController<FingerprintSegmentationModel, String, FingerprintSegmentationModelRepository> {

    public FingerprintSegmentationModelController(FingerprintSegmentationModelRepository repository) {
        super(repository);
    }
}

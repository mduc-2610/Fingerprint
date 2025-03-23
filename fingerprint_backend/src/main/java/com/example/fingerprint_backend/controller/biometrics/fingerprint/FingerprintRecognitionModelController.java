package com.example.fingerprint_backend.controller.biometrics.fingerprint;


import com.example.fingerprint_backend.controller.base.ModelController;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintRecognitionModel;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintRecognitionModelRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fingerprint-recognition")
public class FingerprintRecognitionModelController
        extends ModelController<FingerprintRecognitionModel, String, FingerprintRecognitionModelRepository> {

    public FingerprintRecognitionModelController(FingerprintRecognitionModelRepository repository) {
        super(repository);
    }
}

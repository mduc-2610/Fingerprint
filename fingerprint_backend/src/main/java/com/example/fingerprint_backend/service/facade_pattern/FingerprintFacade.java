package com.example.fingerprint_backend.service.facade_pattern;

import com.example.fingerprint_backend.model.access.AccessLog;
import com.example.fingerprint_backend.model.access.Area;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.model.biometrics.recognition.RecognitionResult;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSegmentationModelRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintRecognitionModelRepository;
import com.example.fingerprint_backend.service.FingerprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FingerprintFacade {
    private final FingerprintService fingerprintService;
    private final FingerprintSegmentationModelRepository segmentationModelRepository;
    private final FingerprintRecognitionModelRepository recognitionModelRepository;

    @Transactional
    public FingerprintSample registerFingerprint(
            String employeeId,
            MultipartFile file,
            String position,
            String segmentationModelId,
            String recognitionModelId) throws Exception {

        String finalSegmentationModelId = Optional.ofNullable(segmentationModelId)
                .orElseGet(() -> {
                    try {
                        return segmentationModelRepository.findLatestModel()
                                .orElseThrow(() -> new Exception("No segmentation model available"))
                                .getId();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        String finalRecognitionModelId = Optional.ofNullable(recognitionModelId)
                .orElseGet(() -> {
                    try {
                        return recognitionModelRepository.findLatestModel()
                                .orElseThrow(() -> new Exception("No recognition model available"))
                                .getId();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        return fingerprintService.registerFingerprint(
                employeeId,
                file,
                position,
                finalSegmentationModelId,
                finalRecognitionModelId
        );
    }

    public RecognitionResult recognizeFingerprint(
            MultipartFile fingerprintImage,
            String segmentationModelId,
            String recognitionModelId) throws Exception {

        String finalSegmentationModelId = Optional.ofNullable(segmentationModelId)
                .orElseGet(() -> {
                    try {
                        return segmentationModelRepository.findLatestModel()
                                .orElseThrow(() -> new Exception("No segmentation model available"))
                                .getId();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        String finalRecognitionModelId = Optional.ofNullable(recognitionModelId)
                .orElseGet(() -> {
                    try {
                        return recognitionModelRepository.findLatestModel()
                                .orElseThrow(() -> new Exception("No recognition model available"))
                                .getId();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        return fingerprintService.recognizeFingerprint(
                fingerprintImage,
                finalSegmentationModelId,
                finalRecognitionModelId
        );
    }

    @Transactional
    public AccessLog createAccessLog(
            MultipartFile fingerprintImage,
            Area area,
            String accessType,
            String segmentationModelId,
            String recognitionModelId) throws Exception {

        RecognitionResult result = recognizeFingerprint(
                fingerprintImage,
                segmentationModelId,
                recognitionModelId
        );

        String finalSegmentationModelId = Optional.ofNullable(segmentationModelId)
                .orElseGet(() -> {
                    try {
                        return segmentationModelRepository.findLatestModel()
                                .orElseThrow(() -> new Exception("No segmentation model available"))
                                .getId();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        String finalRecognitionModelId = Optional.ofNullable(recognitionModelId)
                .orElseGet(() -> {
                    try {
                        return recognitionModelRepository.findLatestModel()
                                .orElseThrow(() -> new Exception("No recognition model available"))
                                .getId();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        return fingerprintService.createAccessLog(
                result.getEmployeeId(),
                area,
                accessType,
                result.getEmployeeId() != null,
                result.getConfidence(),
                finalSegmentationModelId,
                finalRecognitionModelId
        );
    }
}
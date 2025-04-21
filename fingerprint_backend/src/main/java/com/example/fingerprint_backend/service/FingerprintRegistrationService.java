package com.example.fingerprint_backend.service;

import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSegmentationModel;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintRecognitionModel;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSampleRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSegmentationModelRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintRecognitionModelRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FingerprintRegistrationService {
    private static final String VENV_PYTHON = "fingerprint_training/env/Scripts/python.exe"; // Windows
    private static final String PYTHON_SCRIPT_PATH = "fingerprint_training/fingerprint_recognition.py";
    private static final String DATASET_BASE_PATH = "fingerprint_training/fingerprint_adapting_dataset/";

    private final EmployeeRepository employeeRepository;
    private final FingerprintSampleRepository fingerprintSampleRepository;
    private final FingerprintSegmentationModelRepository segmentationModelRepository;
    private final FingerprintRecognitionModelRepository recognitionModelRepository;

    @Transactional
    public FingerprintSample registerFingerprint(
            String employeeId,
            MultipartFile file,
            String position,
            String segmentationModelId,
            String recognitionModelId) throws Exception {

        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        Employee employee = employeeOpt.orElseThrow(() ->
                new Exception("Employee with ID " + employeeId + " not found"));

        Optional<FingerprintSegmentationModel> segModelOpt = segmentationModelRepository.findById(segmentationModelId);
        FingerprintSegmentationModel segmentationModel = segModelOpt.orElseThrow(() ->
                new Exception("Segmentation model with ID " + segmentationModelId + " not found"));

        Optional<FingerprintRecognitionModel> recModelOpt = recognitionModelRepository.findById(recognitionModelId);
        FingerprintRecognitionModel recognitionModel = recModelOpt.orElseThrow(() ->
                new Exception("Recognition model with ID " + recognitionModelId + " not found"));

        String segmentationModelPath = segmentationModel.getPathName();
        String recognitionModelPath = recognitionModel.getPathName();

        String employeeDir = DATASET_BASE_PATH + "/" + employeeId;
        Path employeePath = Paths.get(employeeDir);

        try {
            Files.createDirectories(employeePath);
        } catch (IOException e) {
            throw new Exception("Failed to create directory for employee " + employeeId, e);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".bmp";

        String filename = employeeId + "_" + position + extension;
        Path filePath = Paths.get(employeeDir, filename);

        try {
            byte[] fileBytes = file.getBytes();
            Files.write(filePath, fileBytes);

            FingerprintSample sample = FingerprintSample.builder()
                    .employee(employee)
                    .image(filename)
                    .imageData(fileBytes)
                    .position(position)
                    .capturedAt(LocalDateTime.now())
                    .quality(1.0)
                    .fingerprintRecognitionModel(recognitionModel)
                    .fingerprintSegmentationModel(segmentationModel)
                    .build();

            FingerprintSample savedSample = fingerprintSampleRepository.save(sample);

            updateFingerprintModel(segmentationModelPath, recognitionModelPath);

            return savedSample;

        } catch (IOException e) {
            throw new Exception("Failed to save fingerprint image: " + e.getMessage(), e);
        }
    }

    @Transactional
    public List<FingerprintSample> registerFingerprints(
            String employeeId,
            List<MultipartFile> files,
            List<String> positions,
            String segmentationModelId,
            String recognitionModelId) throws Exception {

        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        Employee employee = employeeOpt.orElseThrow(() ->
                new Exception("Employee with ID " + employeeId + " not found"));

        Optional<FingerprintSegmentationModel> segModelOpt = segmentationModelRepository.findById(segmentationModelId);
        FingerprintSegmentationModel segmentationModel = segModelOpt.orElseThrow(() ->
                new Exception("Segmentation model with ID " + segmentationModelId + " not found"));

        Optional<FingerprintRecognitionModel> recModelOpt = recognitionModelRepository.findById(recognitionModelId);
        FingerprintRecognitionModel recognitionModel = recModelOpt.orElseThrow(() ->
                new Exception("Recognition model with ID " + recognitionModelId + " not found"));

        String segmentationModelPath = segmentationModel.getPathName();
        String recognitionModelPath = recognitionModel.getPathName();

        String employeeDir = DATASET_BASE_PATH + "/" + employeeId;
        Path employeePath = Paths.get(employeeDir);

        try {
            Files.createDirectories(employeePath);
        } catch (IOException e) {
            throw new Exception("Failed to create directory for employee " + employeeId, e);
        }

        List<FingerprintSample> samples = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String position = positions.get(i);

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".bmp";

            String filename = employeeId + "_" + position + extension;
            Path filePath = Paths.get(employeeDir, filename);
            String relativePath = employeeId + "/" + filename;

            try {
                byte[] fileBytes = file.getBytes();
                Files.write(filePath, fileBytes);

                FingerprintSample sample = FingerprintSample.builder()
                        .employee(employee)
                        .image(filename)
                        .imageData(fileBytes)
                        .position(position)
                        .capturedAt(LocalDateTime.now())
                        .quality(1.0)
                        .fingerprintRecognitionModel(recognitionModel)
                        .fingerprintSegmentationModel(segmentationModel)
                        .build();

                samples.add(fingerprintSampleRepository.save(sample));

            } catch (IOException e) {
                throw new Exception("Failed to save fingerprint image: " + e.getMessage(), e);
            }
        }

        employeeRepository.save(employee);

        updateFingerprintModel(segmentationModelPath, recognitionModelPath);

        return samples;
    }

    private void updateFingerprintModel(String segmentationModelPath, String recognitionModelPath)
            throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder(
                VENV_PYTHON,
                PYTHON_SCRIPT_PATH,
                "--update-model",
                "--seg-path-name", segmentationModelPath,
                "--rec-path-name", recognitionModelPath
        );

        Process process = processBuilder.start();
        String errorOutput = new String(process.getErrorStream().readAllBytes());

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Python script execution failed with exit code: "
                    + exitCode + ", Error: " + errorOutput);
        }
    }
}
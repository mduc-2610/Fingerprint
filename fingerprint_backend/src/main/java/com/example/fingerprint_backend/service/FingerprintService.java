package com.example.fingerprint_backend.service;

import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.model.biometrics.recognition.RecognitionResult;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSegmentationModel;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintRecognitionModel;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSampleRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSegmentationModelRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintRecognitionModelRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FingerprintService {
    private static final String VENV_PYTHON = "fingerprint_training/env/Scripts/python.exe"; // Windows
    private static final String RECOGNITION_RESULT_FILE = "recognition_result.json";
    private static final String SCRIPT_DIR = "fingerprint_training/reports";
    private static final String PYTHON_SCRIPT_PATH = "fingerprint_training/fingerprint_recognition.py";
    private static final String DATASET_BASE_PATH = "fingerprint_training/fingerprint_adapting_dataset/";

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private FingerprintSampleRepository fingerprintSampleRepository;

    @Autowired
    private FingerprintSegmentationModelRepository segmentationModelRepository;

    @Autowired
    private FingerprintRecognitionModelRepository recognitionModelRepository;

    @SneakyThrows
    public List<String> registerFingerprints(String employeeId, List<MultipartFile> files,
                                             String segmentationModelId, String recognitionModelId) {
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

        String employeeDir = DATASET_BASE_PATH + employeeId;
        Path employeePath = Paths.get(employeeDir);

        try {
            Files.createDirectories(employeePath);
        } catch (IOException e) {
            throw new Exception("Failed to create directory for employee " + employeeId, e);
        }

        List<String> savedFilePaths = new ArrayList<>();
        List<FingerprintSample> samples = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".bmp";

            String filename = employeeId + "_" + (i + 1) + extension;
            Path filePath = Paths.get(employeeDir, filename);
            String relativePath = employeeId + "/" + filename;

            try {
                byte[] fileBytes = file.getBytes();
                Files.write(filePath, fileBytes);
                savedFilePaths.add(filePath.toString());

                FingerprintSample sample = FingerprintSample.builder()
                        .id(UUID.randomUUID().toString())
                        .employee(employee)
                        .image(filePath.toString())
                        .imageData(fileBytes)
                        .position("finger_" + (i + 1))
                        .capturedAt(LocalDateTime.now())
                        .quality(1.0)
                        .relativePath(relativePath)
                        .build();

                samples.add(sample);

            } catch (IOException e) {
                throw new Exception("Failed to save fingerprint image", e);
            }
        }

//        fingerprintSampleRepository.saveAll(samples);
//
//        if (employee.getFingerprintDatasetId() == null) {
//            employee.setFingerprintDatasetId(employeeId);
//            employeeRepository.save(employee);
//        }

        updateFingerprintModel(segmentationModelPath, recognitionModelPath);

        return savedFilePaths;
    }

    public RecognitionResult recognizeFingerprint(MultipartFile fingerprintImage,
                                                  String segmentationModelId,
                                                  String recognitionModelId) throws Exception {
        // Find models from specific repositories
        Optional<FingerprintSegmentationModel> segModelOpt = segmentationModelRepository.findById(segmentationModelId);
        FingerprintSegmentationModel segmentationModel = segModelOpt.orElseThrow(() ->
                new Exception("Segmentation model with ID " + segmentationModelId + " not found"));

        Optional<FingerprintRecognitionModel> recModelOpt = recognitionModelRepository.findById(recognitionModelId);
        FingerprintRecognitionModel recognitionModel = recModelOpt.orElseThrow(() ->
                new Exception("Recognition model with ID " + recognitionModelId + " not found"));

        String segmentationModelPath = segmentationModel.getPathName();
        String recognitionModelPath = recognitionModel.getPathName();

        String tempFilePath = "temp_" + UUID.randomUUID().toString() + ".bmp";
        Path tempPath = Paths.get(tempFilePath);

        try {
            Files.write(tempPath, fingerprintImage.getBytes());
            System.out.println("Fingerprint saved temporarily to: " + tempPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save temporary fingerprint image: " + e.getMessage());
            throw new Exception("Failed to save temporary fingerprint image", e);
        }

        try {
            System.out.println("Calling recognition script for file: " + tempFilePath);
            executeRecognitionScript(tempFilePath, segmentationModelPath, recognitionModelPath);

            return readRecognitionResultFromFile();

        } catch (Exception e) {
            System.err.println("Error in fingerprint recognition process: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Failed to recognize fingerprint: " + e.getMessage(), e);
        } finally {
            try {
                boolean deleted = Files.deleteIfExists(tempPath);
                System.out.println("Temporary file deleted: " + deleted);
            } catch (IOException e) {
                System.err.println("Failed to delete temporary file: " + e.getMessage());
            }
        }
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

    private String executeRecognitionScript(String imagePath, String segmentationModelPath, String recognitionModelPath)
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                VENV_PYTHON,
                PYTHON_SCRIPT_PATH,
                "--recognize",
                imagePath,
                "--seg-path-name", segmentationModelPath,
                "--rec-path-name", recognitionModelPath
        );

        Process process = processBuilder.start();

        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String errorOutput = new String(process.getErrorStream().readAllBytes());
            throw new IOException("Python script execution failed: " + errorOutput);
        }

        return output;
    }

    private RecognitionResult readRecognitionResultFromFile() throws IOException {
        File resultFile = Paths.get(SCRIPT_DIR, RECOGNITION_RESULT_FILE).toFile();

        if (!resultFile.exists()) {
            throw new IOException("Recognition result file not found");
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(resultFile);

        if (rootNode.has("error")) {
            throw new IOException("Recognition error: " + rootNode.get("error").asText());
        }

        JsonNode similarityNode = rootNode.get("similarity");

        String employeeId = null;
        double confidence = 0.0;

        if (similarityNode != null) {
            JsonNode employeeIdNode = similarityNode.get("employee_id");
            if (employeeIdNode != null && !employeeIdNode.isNull()) {
                employeeId = employeeIdNode.asText();
            }

            confidence = similarityNode.get("confidence").asDouble();
        }

        System.out.println("Successfully parsed result from file: " +
                "employeeId=" + employeeId +
                ", confidence=" + confidence);

        return new RecognitionResult(employeeId, confidence);
    }
}
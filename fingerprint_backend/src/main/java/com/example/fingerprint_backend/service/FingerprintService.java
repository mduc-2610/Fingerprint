package com.example.fingerprint_backend.service;

import com.example.fingerprint_backend.model.access.AccessLog;
import com.example.fingerprint_backend.model.access.Area;
import com.example.fingerprint_backend.model.access.CameraImage;
import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSegmentationModel;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintRecognitionModel;
import com.example.fingerprint_backend.model.biometrics.recognition.Recognition;
import com.example.fingerprint_backend.model.biometrics.recognition.RecognitionResult;
import com.example.fingerprint_backend.repository.access.AccessLogRepository;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSampleRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSegmentationModelRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintRecognitionModelRepository;
import com.example.fingerprint_backend.repository.biometrics.recognition.RecognitionRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
@RequiredArgsConstructor
public class FingerprintService {
    private static final String VENV_PYTHON = "fingerprint_training/env/Scripts/python.exe"; // Windows
    private static final String SCRIPT_DIR = "fingerprint_training/reports";
    private static final String PYTHON_SCRIPT_PATH = "fingerprint_training/fingerprint_recognition.py";
    private static final String DATASET_BASE_PATH = "fingerprint_training/fingerprint_adapting_dataset/";
    private static final String RECOGNITION_RESULT_FILE = "recognition_result.json";

    private final EmployeeRepository employeeRepository;
    private final FingerprintSampleRepository fingerprintSampleRepository;
    private final FingerprintSegmentationModelRepository segmentationModelRepository;
    private final FingerprintRecognitionModelRepository recognitionModelRepository;
    private final RecognitionRepository recognitionRepository;
    private final AccessLogRepository accessLogRepository;

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

    public RecognitionResult recognizeFingerprint(
            MultipartFile fingerprintImage,
            String segmentationModelId,
            String recognitionModelId) throws Exception {

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

    @Transactional
    public AccessLog createAccessLog(
            String employeeId,
            Area area,
            String accessType,
            boolean isMatched,
            double confidence,
            String segmentationModelId,
            String recognitionModelId) {

        LocalDateTime now = LocalDateTime.now();

        AccessLog accessLog = AccessLog.builder()
                .area(area)
                .timestamp(now)
                .accessType(accessType)
                .build();

        if (isMatched && employeeId != null) {
            Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);

            if (employeeOpt.isPresent()) {
                Employee employee = employeeOpt.get();
                accessLog.setEmployee(employee);

                boolean authorized = determineAuthorization(employee, area);
                accessLog.setAuthorized(authorized);
            } else {
                accessLog.setAuthorized(false);
            }
        } else {
            accessLog.setAuthorized(false);
        }

        AccessLog savedAccessLog = accessLogRepository.save(accessLog);

        try {
            FingerprintSegmentationModel segmentationModel = segmentationModelRepository.findById(segmentationModelId)
                    .orElseThrow(() -> new Exception("Segmentation model not found"));

            FingerprintRecognitionModel recognitionModel = recognitionModelRepository.findById(recognitionModelId)
                    .orElseThrow(() -> new Exception("Recognition model not found"));

            Employee employee = null;
            if (employeeId != null) {
                employee = employeeRepository.findById(employeeId).orElse(null);
            }

            Recognition recognition = Recognition.builder()
                    .employee(employee)
                    .accessLog(savedAccessLog) // Link to access log
                    .fingerprintSegmentationModel(segmentationModel)
                    .fingerprintRecognitionModel(recognitionModel)
                    .timestamp(now)
                    .confidence((float) confidence)
                    .build();

            recognitionRepository.save(recognition);

        } catch (Exception e) {
            System.err.println("Failed to create recognition record: " + e.getMessage());
        }

        return savedAccessLog;
    }

    private boolean determineAuthorization(Employee employee, Area area) {
        if (area == null) {
            return true;
        }

        if (employee == null) {
            return false;
        }

        return true;
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
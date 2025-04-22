package com.example.fingerprint_backend.service;

import com.example.fingerprint_backend.model.access.AccessLog;
import com.example.fingerprint_backend.model.access.Area;
import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSegmentationModel;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintRecognitionModel;
import com.example.fingerprint_backend.model.biometrics.recognition.Recognition;
import com.example.fingerprint_backend.model.biometrics.recognition.RecognitionResult;
import com.example.fingerprint_backend.repository.access.AccessLogRepository;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FingerprintRecognitionService {
    private static final String VENV_PYTHON = "fingerprint_training/env/Scripts/python.exe"; // Windows
    private static final String SCRIPT_DIR = "fingerprint_training/reports";
    private static final String PYTHON_SCRIPT_PATH = "fingerprint_training/fingerprint_recognition.py";
    private static final String RECOGNITION_RESULT_FILE = "recognition_result.json";

    private final EmployeeRepository employeeRepository;
    private final FingerprintSegmentationModelRepository segmentationModelRepository;
    private final FingerprintRecognitionModelRepository recognitionModelRepository;
    private final RecognitionRepository recognitionRepository;
    private final AccessLogRepository accessLogRepository;
    private final AreaAccessValidationService areaAccessValidationService;

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
    public Map<String, Object> processRecognition(
            MultipartFile fingerprintImage,
            String segmentationModelId,
            String recognitionModelId,
            Area area,
            String accessType) throws Exception {

        Map<String, Object> response = new HashMap<>();

        RecognitionResult result = recognizeFingerprint(
                fingerprintImage,
                segmentationModelId,
                recognitionModelId);

        if (result == null) {
            throw new Exception("Fingerprint recognition failed");
        }

        AccessLog accessLog = createAccessLog(
                result.getEmployeeId(),
                area,
                accessType,
                result.isMatch(),
                result.getConfidence(),
                segmentationModelId,
                recognitionModelId);

        response.put("matched", result.isMatch());
        response.put("confidence", result.getConfidence());
        response.put("accessLog", accessLog);
        response.put("authorized", accessLog.isAuthorized());

        if (result.isMatch()) {
            response.put("employeeId", result.getEmployeeId());

            Optional<Employee> employee = employeeRepository.findById(result.getEmployeeId());
            if (employee.isPresent()) {
                response.put("employee", employee.get());
            } else {
                response.put("message", "Employee not found in the database but fingerprint matched");
            }
        }

        return response;
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
            // If no area specified, access is granted (system-level access)
            return true;
        }

        if (employee == null) {
            // No employee identified, no access
            return false;
        }

        // Validate access using the access validation service
        return areaAccessValidationService.validateAccess(employee, area);
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
package com.example.fingerprint_backend.service;

import com.example.fingerprint_backend.custom_exception.FingerprintProcessingException;
import com.example.fingerprint_backend.model.Employee;
import com.example.fingerprint_backend.model.FingerprintSample;
import com.example.fingerprint_backend.model.RecognitionResult;
import com.example.fingerprint_backend.repository.EmployeeRepository;
import com.example.fingerprint_backend.repository.FingerprintSampleRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    private static final String SCRIPT_DIR = "fingerprint_training/fingerprint_results";
    private static final String PYTHON_SCRIPT_PATH = "fingerprint_training/fingerprint_recognition.py";
    private static final String DATASET_BASE_PATH = "fingerprint_training/fingerprint_adapting_dataset/";

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private FingerprintSampleRepository fingerprintSampleRepository;

    /**
     * Registers new fingerprint images for an employee
     *
     * @param employeeId The ID of the employee
     * @param files List of fingerprint image files
     * @return List of paths where images were saved
     */
    @SneakyThrows
    public List<String> registerFingerprints(String employeeId, List<MultipartFile> files) throws FingerprintProcessingException {
        // Check if employee exists in database
        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        Employee employee = employeeOpt.orElseThrow(() ->
                new FingerprintProcessingException("Employee with ID " + employeeId + " not found"));

        // Create directory for employee fingerprints
        String employeeDir = DATASET_BASE_PATH + employeeId;
        Path employeePath = Paths.get(employeeDir);

        try {
            Files.createDirectories(employeePath);
        } catch (IOException e) {
            throw new FingerprintProcessingException("Failed to create directory for employee " + employeeId, e);
        }

        List<String> savedFilePaths = new ArrayList<>();
        List<FingerprintSample> samples = new ArrayList<>();

        // Save each fingerprint image
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".bmp";

            // Format: employeeId_fingerprintNumber.extension
            String filename = employeeId + "_" + (i + 1) + extension;
            Path filePath = Paths.get(employeeDir, filename);
            String relativePath = employeeId + "/" + filename;

            try {
                byte[] fileBytes = file.getBytes();
                Files.write(filePath, fileBytes);
                savedFilePaths.add(filePath.toString());

                // Create and save fingerprint sample record
                FingerprintSample sample = FingerprintSample.builder()
                        .id(UUID.randomUUID().toString())
                        .employee(employee)
                        .image(filePath.toString())
                        .imageData(fileBytes)
                        .position("finger_" + (i + 1)) // Default position naming
                        .capturedAt(LocalDateTime.now())
                        .quality(1.0) // Default quality
                        .relativePath(relativePath)
                        .build();

                samples.add(sample);

            } catch (IOException e) {
                throw new FingerprintProcessingException("Failed to save fingerprint image", e);
            }
        }

        // Save all fingerprint samples to database
//        fingerprintSampleRepository.saveAll(samples);

//        if (employee.getFingerprintDatasetId() == null) {
//            employee.setFingerprintDatasetId(employeeId);
//            employeeRepository.save(employee);
//        }

        // Update the fingerprint model with new data
            updateFingerprintModel();

        return savedFilePaths;
    }

    /**
     * Recognizes an employee from a fingerprint image
     *
     * @param fingerprintImage The fingerprint image file
     * @return Recognition result containing employee ID and confidence
     */
    /**
     * Recognize a fingerprint from an uploaded image file
     */
    public RecognitionResult recognizeFingerprint(MultipartFile fingerprintImage) throws FingerprintProcessingException {
        // Save the uploaded fingerprint to a temporary location
        String tempFilePath = "temp_" + UUID.randomUUID().toString() + ".bmp";
        Path tempPath = Paths.get(tempFilePath);

        try {
            Files.write(tempPath, fingerprintImage.getBytes());
            System.out.println("Fingerprint saved temporarily to: " + tempPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save temporary fingerprint image: " + e.getMessage());
            throw new FingerprintProcessingException("Failed to save temporary fingerprint image", e);
        }

        try {
            // Call Python script to recognize the fingerprint
            System.out.println("Calling recognition script for file: " + tempFilePath);
            executeRecognitionScript(tempFilePath);

            // Read the recognition result from the JSON file
            return readRecognitionResultFromFile();

        } catch (Exception e) {
            System.err.println("Error in fingerprint recognition process: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new FingerprintProcessingException("Failed to recognize fingerprint: " + e.getMessage(), e);
        } finally {
            // Clean up the temporary file
            try {
                boolean deleted = Files.deleteIfExists(tempPath);
                System.out.println("Temporary file deleted: " + deleted);
            } catch (IOException e) {
                System.err.println("Failed to delete temporary file: " + e.getMessage());
            }
        }
    }
    /**
     * Updates the fingerprint model with new data
     */
    private void updateFingerprintModel() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                VENV_PYTHON,
                PYTHON_SCRIPT_PATH,
                "--update-model"
        );

        Process process = processBuilder.start();
        String errorOutput = new String(process.getErrorStream().readAllBytes());

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Python script execution failed with exit code: "
                    + exitCode + ", Error: " + errorOutput);
        }
    }

    /**
     * Executes the Python script to recognize a fingerprint
     *
     * @param imagePath Path to the fingerprint image
     * @return Raw recognition result as a string
     */
    private String executeRecognitionScript(String imagePath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                VENV_PYTHON,
                PYTHON_SCRIPT_PATH,
                "--recognize",
                imagePath
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

    /**
     * Parses the recognition result from the Python script output
     *
     * @param rawResult Raw result from the Python script
     * @return Structured recognition result
     */
    private RecognitionResult readRecognitionResultFromFile() throws IOException {
        File resultFile = Paths.get(SCRIPT_DIR, RECOGNITION_RESULT_FILE).toFile();

        if (!resultFile.exists()) {
            throw new IOException("Recognition result file not found");
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(resultFile);

        // Check if there was an error
        if (rootNode.has("error")) {
            throw new IOException("Recognition error: " + rootNode.get("error").asText());
        }

        // Parse similarity data
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
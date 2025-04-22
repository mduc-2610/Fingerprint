package com.example.fingerprint_backend.service;

import com.example.fingerprint_backend.model.access.AccessLog;
import com.example.fingerprint_backend.model.access.Area;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FingerprintService {
    // URL của Fingerprint API
    @Value("${fingerprint.api.url:http://localhost:5000}")
    private String fingerprintApiUrl;
    
    private final EmployeeRepository employeeRepository;
    private final FingerprintSampleRepository fingerprintSampleRepository;
    private final FingerprintSegmentationModelRepository segmentationModelRepository;
    private final FingerprintRecognitionModelRepository recognitionModelRepository;
    private final RecognitionRepository recognitionRepository;
    private final AccessLogRepository accessLogRepository;

    @Autowired
    private final RestTemplate restTemplate;

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

        try {
            byte[] fileBytes = file.getBytes();
            
            // Lưu mẫu vân tay vào cơ sở dữ liệu
            FingerprintSample sample = FingerprintSample.builder()
                    .employee(employee)
                    .image(file.getOriginalFilename())
                    .imageData(fileBytes)
                    .position(position)
                    .capturedAt(LocalDateTime.now())
                    .quality(1.0)
                    .build();

            FingerprintSample savedSample = fingerprintSampleRepository.save(sample);

            // Gửi yêu cầu đăng ký vân tay đến API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("employee_id", employeeId);
            body.add("position", position);
            body.add("segmentation_model_path", segmentationModelPath);
            body.add("recognition_model_path", recognitionModelPath);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                    fingerprintApiUrl + "/api/register", 
                    requestEntity, 
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return savedSample;
            } else {
                throw new Exception("Failed to register fingerprint: " + response.getBody());
            }

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

        List<FingerprintSample> samples = new ArrayList<>();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Lưu và đăng ký từng mẫu vân tay
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String position = positions.get(i);
                byte[] fileBytes = file.getBytes();

                // Lưu mẫu vào cơ sở dữ liệu
                FingerprintSample sample = FingerprintSample.builder()
                        .employee(employee)
                        .image(file.getOriginalFilename())
                        .imageData(fileBytes)
                        .position(position)
                        .capturedAt(LocalDateTime.now())
                        .quality(1.0)
                        .build();

                samples.add(fingerprintSampleRepository.save(sample));

                // Thêm file vào yêu cầu API
                body.add("file", new ByteArrayResource(fileBytes) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                });
                body.add("position", position);
            }
            
            // Thêm các thông tin khác vào yêu cầu API
            body.add("employee_id", employeeId);
            body.add("segmentation_model_path", segmentationModelPath);
            body.add("recognition_model_path", recognitionModelPath);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                    fingerprintApiUrl + "/api/register", 
                    requestEntity, 
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new Exception("Failed to register fingerprints: " + response.getBody());
            }

            return samples;

        } catch (IOException e) {
            throw new Exception("Failed to save fingerprint images: " + e.getMessage(), e);
        }
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

        try {
            byte[] fileBytes = fingerprintImage.getBytes();
            
            // Chuẩn bị yêu cầu HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fingerprintImage.getOriginalFilename();
                }
            });
            body.add("segmentation_model_path", segmentationModelPath);
            body.add("recognition_model_path", recognitionModelPath);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Gửi yêu cầu đến API
            ResponseEntity<String> response = restTemplate.postForEntity(
                    fingerprintApiUrl + "/api/recognize", 
                    requestEntity, 
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.getBody());
                
                if (rootNode.has("error")) {
                    throw new Exception("Recognition error: " + rootNode.get("error").asText());
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
                
                System.out.println("Successfully recognized: employeeId=" + employeeId + ", confidence=" + confidence);
                
                return new RecognitionResult(employeeId, confidence);
            } else {
                throw new Exception("Failed to recognize fingerprint: " + response.getBody());
            }
        } catch (IOException e) {
            System.err.println("Error in fingerprint recognition process: " + e.getMessage());
            e.printStackTrace();
            throw new Exception("Failed to recognize fingerprint: " + e.getMessage(), e);
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

        return true; // Đây là logic đơn giản, có thể thay thế bằng logic phức tạp hơn theo nhu cầu
    }

    private void updateFingerprintModel(String segmentationModelPath, String recognitionModelPath)
            throws IOException {
        // Chuẩn bị dữ liệu để gọi API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Tạo body request
        ObjectMapper mapper = new ObjectMapper();
        String requestJson = mapper.writeValueAsString(
            mapper.createObjectNode()
                .put("segmentation_model_path", segmentationModelPath)
                .put("recognition_model_path", recognitionModelPath)
        );
        
        HttpEntity<String> requestEntity = new HttpEntity<>(requestJson, headers);
        
        // Gọi API và xử lý kết quả
        ResponseEntity<String> response = restTemplate.postForEntity(
                fingerprintApiUrl + "/api/update-model", 
                requestEntity, 
                String.class
        );
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            JsonNode rootNode = mapper.readTree(response.getBody());
            String errorMessage = "Unknown error";
            
            if (rootNode.has("message")) {
                errorMessage = rootNode.get("message").asText();
            } else if (rootNode.has("error")) {
                errorMessage = rootNode.get("error").asText();
            }
            
            throw new IOException("Failed to update fingerprint model: " + errorMessage);
        }
    }
}
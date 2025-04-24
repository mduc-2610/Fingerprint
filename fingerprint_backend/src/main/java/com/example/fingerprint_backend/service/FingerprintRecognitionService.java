package com.example.fingerprint_backend.service;

import com.example.fingerprint_backend.model.access.AccessLog;
import com.example.fingerprint_backend.model.access.Area;
import com.example.fingerprint_backend.model.access.AreaAccess;
import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSegmentationModel;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintRecognitionModel;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.model.biometrics.recognition.Recognition;
import com.example.fingerprint_backend.model.biometrics.recognition.RecognitionResult;
import com.example.fingerprint_backend.repository.access.AccessLogRepository;
import com.example.fingerprint_backend.repository.access.AreaAccessRepository;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSegmentationModelRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintRecognitionModelRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSampleRepository;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FingerprintRecognitionService {

    @Value("${fingerprint.api.url:http://localhost:5000}")
    private String fingerprintApiUrl;

    private final EmployeeRepository employeeRepository;
    private final FingerprintSegmentationModelRepository segmentationModelRepository;
    private final FingerprintRecognitionModelRepository recognitionModelRepository;
    private final RecognitionRepository recognitionRepository;
    private final AccessLogRepository accessLogRepository;
    private final AreaAccessRepository areaAccessRepository;
    private final FingerprintSampleRepository fingerprintSampleRepository;

    @Autowired
    private final RestTemplate restTemplate;

    public RecognitionResult recognizeFingerprint(
            MultipartFile fingerprintImage,
            String segmentationModelId,
            String recognitionModelId) throws Exception {

        Optional<FingerprintSegmentationModel> segModelOpt = segmentationModelRepository.findById(segmentationModelId);
        FingerprintSegmentationModel segmentationModel = segModelOpt
                .orElseThrow(() -> new Exception("Segmentation model with ID " + segmentationModelId + " not found"));

        Optional<FingerprintRecognitionModel> recModelOpt = recognitionModelRepository.findById(recognitionModelId);
        FingerprintRecognitionModel recognitionModel = recModelOpt
                .orElseThrow(() -> new Exception("Recognition model with ID " + recognitionModelId + " not found"));

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
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.getBody());

                if (rootNode.has("error")) {
                    throw new Exception("Recognition error: " + rootNode.get("error").asText());
                }

                JsonNode similarityNode = rootNode.get("similarity");

                String employeeId = null;
                double confidence = 0.0;
                String fingerprintId = null;
                Boolean match = false;

                if (similarityNode != null) {
                    JsonNode employeeIdNode = similarityNode.get("employee_id");
                    if (employeeIdNode != null && !employeeIdNode.isNull()) {
                        employeeId = employeeIdNode.asText();
                    }
                    JsonNode fingerIdNode = similarityNode.get("fingerprint_id");

                    match = similarityNode.get("match").asBoolean();
                    if (fingerIdNode != null && !fingerIdNode.isNull()) {
                        fingerprintId = fingerIdNode.asText();
                    }

                    confidence = similarityNode.get("confidence").asDouble();
                }

                System.out.println("Successfully recognized: employeeId=" + employeeId + ", confidence=" + confidence
                        + " fingerId=" + fingerprintId);

                return new RecognitionResult(employeeId, confidence, fingerprintId, match);
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
                result.getMatch(),
                result.getConfidence(),
                segmentationModelId,
                recognitionModelId);

        response.put("matched", result.getMatch());
        response.put("confidence", result.getConfidence());
        response.put("accessLog", accessLog);
        response.put("authorized", accessLog.isAuthorized());

        if (result.getMatch()) {
            List<AreaAccess> areaAccessList = areaAccessRepository.findByEmployeeId(result.getEmployeeId());
            var isAccessable = false;
            for (AreaAccess areaAccess : areaAccessList) {
                if (areaAccess.getArea().getId().equals(area.getId())) {
                    isAccessable = true;
                    break;
                }
            }
            Boolean isActive = false;
            FingerprintSample fingerprintSample = fingerprintSampleRepository.findById(result.getFingerprintId())
                    .orElseThrow(() -> new Exception("Fingerprint sample not found with id: " + result.getFingerprintId()));
            if(fingerprintSample.isActive()){
                isActive = true;
            }
            response.put("active", isActive);
            response.put("accessable", isAccessable);
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
            return true;
        }

        if (employee == null) {
            return false;
        }

        return true; // Đây là logic đơn giản, có thể thay thế bằng logic phức tạp hơn theo nhu cầu
    }

}
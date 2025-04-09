package com.example.fingerprint_backend.service.command_pattern;

import com.example.fingerprint_backend.model.access.AccessLog;
import com.example.fingerprint_backend.model.access.Area;
import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.model.biometrics.recognition.RecognitionResult;
import com.example.fingerprint_backend.repository.access.AreaRepository;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
import com.example.fingerprint_backend.service.FingerprintService;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RecognizeFingerprintCommand implements FingerprintCommand<Map<String, Object>> {

    private final FingerprintService fingerprintService;
    private final EmployeeRepository employeeRepository;
    private final AreaRepository areaRepository;

    private final MultipartFile file;
    private final String segmentationModelId;
    private final String recognitionModelId;
    private final String areaId;
    private final String accessType;

    public RecognizeFingerprintCommand(
            FingerprintService fingerprintService,
            EmployeeRepository employeeRepository,
            AreaRepository areaRepository,
            MultipartFile file,
            String segmentationModelId,
            String recognitionModelId,
            String areaId,
            String accessType) {
        this.fingerprintService = fingerprintService;
        this.employeeRepository = employeeRepository;
        this.areaRepository = areaRepository;
        this.file = file;
        this.segmentationModelId = segmentationModelId;
        this.recognitionModelId = recognitionModelId;
        this.areaId = areaId;
        this.accessType = accessType;
    }

    @Override
    public Map<String, Object> execute() throws Exception {
        try {
            Area area = null;
            if (areaId != null && !areaId.isEmpty()) {
                Optional<Area> areaOpt = areaRepository.findById(areaId);
                if (areaOpt.isPresent()) {
                    area = areaOpt.get();
                }
            }

            RecognitionResult result = fingerprintService.recognizeFingerprint(file, segmentationModelId, recognitionModelId);

            if (result == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Fingerprint recognition failed");
                return errorResponse;
            }

            AccessLog accessLog = fingerprintService.createAccessLog(
                    result.getEmployeeId(),
                    area,
                    accessType,
                    result.isMatch(),
                    result.getConfidence(),
                    segmentationModelId,
                    recognitionModelId);

            Map<String, Object> response = new HashMap<>();
            response.put("matched", result.isMatch());
            response.put("confidence", result.getConfidence());
            response.put("accessLog", accessLog);
            response.put("authorized", accessLog.isAuthorized());
            response.put("segmentationModelId", segmentationModelId);
            response.put("recognitionModelId", recognitionModelId);

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
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "An unexpected error occurred: " + e.getMessage());
            return errorResponse;
        }
    }
}

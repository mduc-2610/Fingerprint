package com.example.fingerprint_backend.util;

import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.repository.access.AccessLogRepository;
import com.example.fingerprint_backend.repository.access.AreaRepository;
import com.example.fingerprint_backend.repository.access.CameraImageRepository;
import com.example.fingerprint_backend.repository.access.CameraRepository;
import com.example.fingerprint_backend.repository.auth.AdminRepository;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
import com.example.fingerprint_backend.repository.base.TrainingDataRepository;
import com.example.fingerprint_backend.repository.base.UserRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintRecognitionModelRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSampleRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSegmentationModelRepository;
import com.example.fingerprint_backend.repository.biometrics.recognition.RecognitionRepository;
import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DataGenerator {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AdminRepository adminRepository;
    private final AreaRepository areaRepository;
    private final CameraRepository cameraRepository;
    private final AccessLogRepository accessLogRepository;
    private final RecognitionRepository recognitionRepository;
    private final FingerprintSampleRepository fingerprintSampleRepository;
    private final CameraImageRepository cameraImageRepository;
    private final TrainingDataRepository trainingDataRepository;
    private final FingerprintRecognitionModelRepository fingerprintRecognitionModelRepository;
    private final FingerprintSegmentationModelRepository fingerprintSegmentationModelRepository;

    private final Faker faker = new Faker();

    // @PostConstruct
    public void initializeData() throws IOException {

        accessLogRepository.deleteAll();
        fingerprintSampleRepository.deleteAll();
        employeeRepository.deleteAll();
        adminRepository.deleteAll();
        userRepository.deleteAll();
        cameraRepository.deleteAll();
        areaRepository.deleteAll();
        cameraImageRepository.deleteAll();
        trainingDataRepository.deleteAll();
        fingerprintRecognitionModelRepository.deleteAll();
        fingerprintSegmentationModelRepository.deleteAll();
        recognitionRepository.deleteAll();
        if (employeeRepository.count() > 0) {
            return;
        }

        List<Employee> employees = createFakeEmployees(20);

        System.out.println("Initialized data with " + employees.size() + " employees");
    }

    private List<Employee> createFakeEmployees(int count) {
        List<Employee> employees = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Employee employee = Employee.builder()
                    .id(UUID.randomUUID().toString())
                    .fullName(faker.name().fullName())
                    .phoneNumber(faker.phoneNumber().cellPhone())
                    .photo("employee_photo_" + i + ".jpg")
                    .address(faker.address().fullAddress())
                    .maxNumberSamples(5)
                    .build();
            employees.add(employeeRepository.save(employee));
        }

        return employees;
    }

}
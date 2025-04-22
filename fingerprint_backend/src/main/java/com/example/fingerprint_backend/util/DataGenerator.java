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

import java.io.File;
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

        recognitionRepository.deleteAll();
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
        if (employeeRepository.count() > 0) {
            return;
        }

        List<Employee> employees = createEmployeesFromDataset();

        if (employees.isEmpty()) {
            employees = createFakeEmployees(20);
        }
        System.out.println("Initialized data with " + employees.size() + " employees");
    }

    private List<Employee> createEmployeesFromDataset() {
        List<Employee> employees = new ArrayList<>();

        File datasetDir = new File("fingerprint_adapting_test_dataset");
        File[] datasetFolders = datasetDir.listFiles(File::isDirectory);

        if (datasetFolders == null || datasetFolders.length == 0) {
            System.out.println("No dataset folders found in fingerprint_adapting_test_dataset");
            return employees;
        }

        for (File folder : datasetFolders) {

            Employee employee = Employee.builder()
                    .id(UUID.randomUUID().toString())
                    .fullName(faker.name().fullName())
                    .phoneNumber(faker.phoneNumber().cellPhone())
                    .username(faker.name().username())
                    .email(faker.internet().emailAddress())
                    .password(faker.internet().password())
                    .build();

            employee.setFingerprintSamples(new ArrayList<>());
            employees.add(employeeRepository.save(employee));
        }

        System.out.println("Loaded " + employees.size() + " employees with fingerprint samples from dataset");
        return employees;
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
                    .build();
            employees.add(employeeRepository.save(employee));
        }

        return employees;
    }
}
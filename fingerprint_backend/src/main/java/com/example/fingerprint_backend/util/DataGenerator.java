package com.example.fingerprint_backend.util;

import com.example.fingerprint_backend.model.*;
import com.example.fingerprint_backend.repository.*;
import com.example.fingerprint_backend.service.FingerprintService;
import com.github.javafaker.Faker;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    private final FingerprintService fingerprintService;

    private final Faker faker = new Faker();
    private final Random random = new Random();

    @PostConstruct
    public void initializeData() {
        // Only proceed if no employees exist
        if (employeeRepository.count() > 0) {
            return;
        }

        // Create areas
        List<Area> areas = createAreas(5);

        // Create cameras for each area
        List<Camera> cameras = createCameras(areas);

        // Create admins
        List<Admin> admins = createAdmins(3);

        // Create employees with fingerprint dataset
        List<Employee> employees = createEmployeesFromDataset();

        // If no employees were created from dataset, create fake ones
        if (employees.isEmpty()) {
            employees = createFakeEmployees(20);
        }

        // Create employee statistics

        // Create models
        FingerprintRecognitionModel recognitionModel = createFingerprintRecognitionModel();
        FingerprintRegionModel regionModel = createFingerprintRegionModel();

        // Create training data
        TrainingData trainingData = createTrainingData();

        // Create model-training data relations
        createFRegionTData(regionModel, trainingData);
        createFRecognitionTData(recognitionModel, trainingData);

        // Create camera images
        List<CameraImage> cameraImages = createCameraImages(cameras);

        // Create access logs
        List<AccessLog> accessLogs = createAccessLogs(employees, areas);

        // Create recognitions
        createRecognitions(employees, cameraImages, accessLogs, recognitionModel, regionModel);

        System.out.println("Initialized data with " + employees.size() + " employees");
    }

    private List<Employee> createEmployeesFromDataset() {
        List<Employee> employees = new ArrayList<>();

        // Get list of dataset folders
        File datasetDir = new File("fingerprint_adapting_test_dataset");
        File[] datasetFolders = datasetDir.listFiles(File::isDirectory);

        if (datasetFolders == null || datasetFolders.length == 0) {
            System.out.println("No dataset folders found in fingerprint_adapting_test_dataset");
            return employees;
        }

        // Create employees with dataset IDs
        for (File folder : datasetFolders) {
            String datasetId = folder.getName();

            // Create employee
            Employee employee = Employee.builder()
                    .id(UUID.randomUUID().toString())
                    .fullName(faker.name().fullName())
                    .phoneNumber(faker.phoneNumber().cellPhone())
                    .build();

            employee.setFingerprintSamples(new ArrayList<>());
            employees.add(employeeRepository.save(employee));
        }

        System.out.println("Loaded " + employees.size() + " employees with fingerprint samples from dataset");
        return employees;
    }

    private String getRandomFingerPosition() {
        String[] positions = {
                "right_thumb", "right_index", "right_middle", "right_ring", "right_little",
                "left_thumb", "left_index", "left_middle", "left_ring", "left_little"
        };
        return positions[random.nextInt(positions.length)];
    }

    private List<Area> createAreas(int count) {
        List<Area> areas = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Area area = Area.builder()
                    .id(UUID.randomUUID().toString())
                    .name(faker.company().name())
                    .securityLevel(random.nextInt(5) + 1)
                    .description(faker.lorem().sentence())
                    .build();
            areas.add(areaRepository.save(area));
        }
        return areas;
    }

    private List<Camera> createCameras(List<Area> areas) {
        List<Camera> cameras = new ArrayList<>();
        for (Area area : areas) {
            int cameraCount = random.nextInt(3) + 1;
            for (int i = 0; i < cameraCount; i++) {
                Camera camera = Camera.builder()
                        .id(UUID.randomUUID().toString())
                        .area(area)
                        .location(faker.address().streetAddress())
                        .status(random.nextBoolean())
                        .installed_at(LocalDateTime.ofInstant(
                                faker.date().past(365, TimeUnit.DAYS).toInstant(),
                                ZoneId.systemDefault()))
                        .build();
                cameras.add(cameraRepository.save(camera));
            }
        }
        return cameras;
    }

    private List<Admin> createAdmins(int count) {
        List<Admin> admins = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Admin admin = Admin.builder()
                    .id(UUID.randomUUID().toString())
                    .fullName(faker.name().fullName())
                    .phoneNumber(faker.phoneNumber().cellPhone())
                    .photo("admin_photo_" + i + ".jpg")
                    .address(faker.address().fullAddress())
                    .username(faker.name().username())
                    .email(faker.internet().emailAddress())
                    .password(faker.internet().password())
                    .build();
            admins.add(adminRepository.save(admin));
        }
        return admins;
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

        // Create fingerprint samples for these employees
        createFingerprintSamples(employees);

        return employees;
    }

    private List<FingerprintSample> createFingerprintSamples(List<Employee> employees) {
        List<FingerprintSample> samples = new ArrayList<>();

        for (Employee employee : employees) {
            int sampleCount = random.nextInt(3) + 1;
            for (int i = 0; i < sampleCount; i++) {
                FingerprintSample sample = FingerprintSample.builder()
                        .id(UUID.randomUUID().toString())
                        .employee(employee)
                        .image("fingerprint_" + employee.getId() + "_" + i + ".jpg")
                        .imageData(new byte[1024]) // Dummy data
                        .position(getRandomFingerPosition())
                        .capturedAt(LocalDateTime.ofInstant(
                                faker.date().past(30, TimeUnit.DAYS).toInstant(),
                                ZoneId.systemDefault()))
                        .quality(random.nextDouble() * 25 + 75) // 75-100 quality range
                        .relativePath("fake/path/" + employee.getId() + "/" + i + ".jpg")
                        .build();
                samples.add(fingerprintSampleRepository.save(sample));
            }
        }
        return samples;
    }

    private FingerprintRecognitionModel createFingerprintRecognitionModel() {
        FingerprintRecognitionModel model = FingerprintRecognitionModel.builder()
                .id(UUID.randomUUID().toString())
                .name("RecognitionModel-v" + (random.nextInt(5) + 1))
                .accuracy(75.0f + random.nextFloat() * 20.0f)
                .validationScores(80.0f + random.nextFloat() * 15.0f)
                .version(random.nextInt(5) + 1)
                .createdAt(LocalDateTime.ofInstant(
                        faker.date().past(180, TimeUnit.DAYS).toInstant(),
                        ZoneId.systemDefault()))
                .updatedAt(LocalDateTime.ofInstant(
                        faker.date().past(30, TimeUnit.DAYS).toInstant(),
                        ZoneId.systemDefault()))
                .build();
        return model;
    }

    private FingerprintRegionModel createFingerprintRegionModel() {
        FingerprintRegionModel model = FingerprintRegionModel.builder()
                .id(UUID.randomUUID().toString())
                .name("RegionModel-v" + (random.nextInt(5) + 1))
                .accuracy(75.0f + random.nextFloat() * 20.0f)
                .validationScores(80.0f + random.nextFloat() * 15.0f)
                .version(random.nextInt(5) + 1)
                .createdAt(LocalDateTime.ofInstant(
                        faker.date().past(180, TimeUnit.DAYS).toInstant(),
                        ZoneId.systemDefault()))
                .updatedAt(LocalDateTime.ofInstant(
                        faker.date().past(30, TimeUnit.DAYS).toInstant(),
                        ZoneId.systemDefault()))
                .build();
        return model;
    }

    private TrainingData createTrainingData() {
        TrainingData trainingData = TrainingData.builder()
                .id(UUID.randomUUID().toString())
                .name("Training-" + UUID.randomUUID().toString().substring(0, 8))
                .purpose("Model improvement")
                .sampleCount(random.nextInt(5000) + 1000)
                .createdAt(LocalDateTime.ofInstant(
                        faker.date().past(90, TimeUnit.DAYS).toInstant(),
                        ZoneId.systemDefault()))
                .build();
        return trainingData;
    }

    private void createFRegionTData(FingerprintRegionModel model, TrainingData trainingData) {
        FRegionTData data = FRegionTData.builder()
                .id(UUID.randomUUID().toString())
                .fingerprintRegionModel(model)
                .trainingData(trainingData)
                .build();
    }

    private void createFRecognitionTData(FingerprintRecognitionModel model, TrainingData trainingData) {
        FRecognitionTData data = FRecognitionTData.builder()
                .id(UUID.randomUUID().toString())
                .fingerprintRecognitionModel(model)
                .trainingData(trainingData)
                .build();
    }

    private List<CameraImage> createCameraImages(List<Camera> cameras) {
        List<CameraImage> images = new ArrayList<>();
        for (Camera camera : cameras) {
            int imageCount = random.nextInt(5) + 1;
            for (int i = 0; i < imageCount; i++) {
                CameraImage image = CameraImage.builder()
                        .id(UUID.randomUUID().toString())
                        .camera(camera)
                        .image("camera_capture_" + camera.getId() + "_" + i + ".jpg")
                        .imageData(new byte[4096]) // Dummy data
                        .captured_at(LocalDateTime.ofInstant(
                                faker.date().past(7, TimeUnit.DAYS).toInstant(),
                                ZoneId.systemDefault()))
                        .build();
                images.add(image);
            }
        }
        return images;
    }

    private List<AccessLog> createAccessLogs(List<Employee> employees, List<Area> areas) {
        List<AccessLog> logs = new ArrayList<>();
        String[] accessTypes = {"entry", "exit"};

        for (Employee employee : employees) {
            int logCount = random.nextInt(10) + 1;
            for (int i = 0; i < logCount; i++) {
                AccessLog log = AccessLog.builder()
                        .id(UUID.randomUUID().toString())
                        .employee(employee)
                        .area(areas.get(random.nextInt(areas.size())))
                        .timestamp(LocalDateTime.ofInstant(
                                faker.date().past(14, TimeUnit.DAYS).toInstant(),
                                ZoneId.systemDefault()))
                        .isAuthorized(random.nextFloat() > 0.1f) // 90% authorized
                        .accessType(accessTypes[random.nextInt(accessTypes.length)])
                        .build();
                logs.add(accessLogRepository.save(log));
            }
        }
        return logs;
    }

    private void createRecognitions(List<Employee> employees, List<CameraImage> images,
                                    List<AccessLog> logs, FingerprintRecognitionModel recognitionModel,
                                    FingerprintRegionModel regionModel) {
        for (AccessLog log : logs) {
            CameraImage image = images.get(random.nextInt(images.size()));

            Recognition recognition = Recognition.builder()
                    .id(UUID.randomUUID().toString())
                    .cameraImage(image)
                    .fingerprintRecognitionModel(recognitionModel)
                    .fingerprintRegionModel(regionModel)
                    .accessLog(log)
                    .employee(log.getEmployee())
                    .timestamp(log.getTimestamp())
                    .confidence(70.0f + random.nextFloat() * 29.0f)
                    .build();

            recognitionRepository.save(recognition);
        }
    }
}
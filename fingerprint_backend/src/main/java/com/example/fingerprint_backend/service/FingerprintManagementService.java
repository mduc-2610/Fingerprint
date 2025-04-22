package com.example.fingerprint_backend.service;

import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.model.biometrics.fingerprint.FingerprintSample;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
import com.example.fingerprint_backend.repository.biometrics.fingerprint.FingerprintSampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FingerprintManagementService {

    private static final String ADAPTING_DATASET_BASE_PATH = "fingerprint_training/fingerprint_adapting_dataset/";
    private static final String DISABLED_BASE_PATH = "fingerprint_training/fingerprint_disabled/";

    @Autowired
    private FingerprintSampleRepository fingerprintSampleRepository;

    @Autowired
    private EmployeeRepository employeeRepository;


    public void moveFileToDisabledDirectory(String image, String employeeId) throws IOException {
        Path disabledDir = Paths.get(DISABLED_BASE_PATH + employeeId);
        Files.createDirectories(disabledDir);

        Path sourcePath = Paths.get(ADAPTING_DATASET_BASE_PATH + employeeId + "/" + image);
        Path destPath = disabledDir.resolve(image);

        Files.move(sourcePath, destPath);
    }

    public void moveFileToAdaptingDirectory(String image, String employeeId) throws IOException {
        Path adaptingDir = Paths.get(ADAPTING_DATASET_BASE_PATH + employeeId);
        Files.createDirectories(adaptingDir);

        Path sourcePath = Paths.get(DISABLED_BASE_PATH + employeeId + "/" + image);
        Path destPath = adaptingDir.resolve(image);

        Files.move(sourcePath, destPath);
    }

    public void moveAllActiveFilesToDisabledDirectory(List<FingerprintSample> activeSamples, String employeeId) throws IOException {
        Path disabledDir = Paths.get(DISABLED_BASE_PATH + employeeId);
        Files.createDirectories(disabledDir);

        for (FingerprintSample sample : activeSamples) {
            Path sourcePath = Paths.get(ADAPTING_DATASET_BASE_PATH + employeeId + "/" + sample.getImage());
            Path destPath = disabledDir.resolve(sample.getImage());

            if (Files.exists(sourcePath)) {
                Files.move(sourcePath, destPath);
            }
        }
    }

    public void deleteFingerprintImage(String image, String employeeId) throws IOException {
        Path adaptingPath = Paths.get(ADAPTING_DATASET_BASE_PATH + employeeId + "/" + image);
        Path disabledPath = Paths.get(DISABLED_BASE_PATH + employeeId + "/" + image);

        if (Files.exists(adaptingPath)) {
            Files.delete(adaptingPath);
        } else if (Files.exists(disabledPath)) {
            Files.delete(disabledPath);
        } else {
            throw new IOException("Image file not found in both adapting and disabled directories.");
        }
    }

}
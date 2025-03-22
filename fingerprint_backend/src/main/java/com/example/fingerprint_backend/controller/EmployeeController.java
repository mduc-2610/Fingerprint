package com.example.fingerprint_backend.controller;

import com.example.fingerprint_backend.dto.EmployeeDTO;
import com.example.fingerprint_backend.model.Employee;
import com.example.fingerprint_backend.model.FingerprintSample;
import com.example.fingerprint_backend.repository.EmployeeRepository;
import com.example.fingerprint_backend.repository.FingerprintSampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final FingerprintSampleRepository fingerprintSampleRepository;

    @GetMapping
    public List<EmployeeDTO> getAllEmployees() {
        return employeeRepository.findAllEmployeesDTO();
    }


    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable String id) {
        Optional<EmployeeDTO> employee = Optional.ofNullable(employeeRepository.findByIdDTO(id));
        return employee.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Employee createEmployee(@RequestBody Employee employee) {
        return employeeRepository.save(employee);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable String id, @RequestBody Employee employee) {
        if (!employeeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        employee.setId(id);
        return ResponseEntity.ok(employeeRepository.save(employee));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String id) {
        if (!employeeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        employeeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/fingerprint-samples")
    public ResponseEntity<List<FingerprintSample>> getEmployeeFingerprintSamples(@PathVariable String id) {
        if (!employeeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<FingerprintSample> samples = fingerprintSampleRepository.findByEmployeeId(id);
        return ResponseEntity.ok(samples);
    }
}

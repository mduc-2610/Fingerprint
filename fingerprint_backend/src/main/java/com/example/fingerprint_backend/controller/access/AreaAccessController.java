package com.example.fingerprint_backend.controller.access;

import com.example.fingerprint_backend.model.access.Area;
import com.example.fingerprint_backend.model.access.AreaAccess;
import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.repository.access.AreaRepository;
import com.example.fingerprint_backend.repository.access.AreaAccessRepository;
import com.example.fingerprint_backend.repository.auth.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/access")
public class AreaAccessController {
        @Autowired
        private AreaAccessRepository areaAccessRepository;

        @Autowired
        private EmployeeRepository employeeRepository;

        @Autowired
        private AreaRepository areaRepository;

        @GetMapping("/by-employee/{employeeId}")
        public ResponseEntity<List<AreaAccess>> getAccessByEmployee(@PathVariable String employeeId) {
                return ResponseEntity.ok(areaAccessRepository.findByEmployeeId(employeeId));
        }

        @PostMapping("/grant")
        public ResponseEntity<Map<String, Object>> grantAccess(
                        @RequestParam String employeeId,
                        @RequestParam String areaId) {

                Employee employee = employeeRepository.findById(employeeId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Employee not found with id: " + employeeId));

                Area area = areaRepository.findById(areaId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Area not found with id: " + areaId));

                boolean accessExists = areaAccessRepository.existsByEmployeeAndArea(employee, area);
                if (accessExists) {
                        throw new ResponseStatusException(
                                        HttpStatus.CONFLICT, "Employee already has access to this area");
                }

                AreaAccess accessPermission = AreaAccess.builder()
                                .employee(employee)
                                .area(area)
                                .timestamp(LocalDateTime.now())
                                .build();

                AreaAccess savedAccess = areaAccessRepository.save(accessPermission);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Access granted successfully");
                response.put("accessId", savedAccess.getId());
                response.put("employeeId", employee.getId());
                response.put("employeeName", employee.getFullName());
                response.put("areaId", area.getId());
                response.put("areaName", area.getName());
                response.put("timestamp", savedAccess.getTimestamp());

                return ResponseEntity.ok(response);
        }

        @PostMapping("/grantAllAreas")
        public ResponseEntity<Map<String, Object>> grantAccessToAllAreas(@RequestParam String employeeId) {
                Employee employee = employeeRepository.findById(employeeId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Employee not found with id: " + employeeId));

                List<Area> areas = areaRepository.findAll();
                for (Area area : areas) {
                        boolean accessExists = areaAccessRepository.existsByEmployeeAndArea(employee, area);
                        if (!accessExists) {
                                AreaAccess accessPermission = AreaAccess.builder()
                                                .employee(employee)
                                                .area(area)
                                                .timestamp(LocalDateTime.now())
                                                .build();
                                areaAccessRepository.save(accessPermission);
                        }
                }

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Access granted to all areas successfully");
                response.put("employeeId", employee.getId());
                response.put("employeeName", employee.getFullName());

                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/revoke/{accessId}")
        public ResponseEntity<Map<String, Object>> revokeAccess(@PathVariable String accessId) {
                if (!areaAccessRepository.existsById(accessId)) {
                        throw new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Access permission not found with id: " + accessId);
                }

                areaAccessRepository.deleteById(accessId);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Access permission revoked successfully");
                response.put("accessId", accessId);

                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/revoke")
        public ResponseEntity<Map<String, Object>> revokeAccessByEmployeeAndArea(
                        @RequestParam String employeeId,
                        @RequestParam String areaId) {

                Employee employee = employeeRepository.findById(employeeId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Employee not found with id: " + employeeId));

                Area area = areaRepository.findById(areaId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Area not found with id: " + areaId));

                Optional<AreaAccess> existingAccess = areaAccessRepository.findByEmployeeAndArea(employee, area);

                if (existingAccess.isEmpty()) {
                        throw new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "No access permission found for employee id " + employeeId
                                                        + " and area id " + areaId);
                }

                int deletedCount = areaAccessRepository.deleteByEmployeeIdAndAreaId(employeeId, areaId);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Access permissions revoked successfully");
                response.put("employeeId", employeeId);
                response.put("employeeName", employee.getFullName());
                response.put("areaId", areaId);
                response.put("areaName", area.getName());
                response.put("removedPermissions", deletedCount);

                return ResponseEntity.ok(response);
        }
}

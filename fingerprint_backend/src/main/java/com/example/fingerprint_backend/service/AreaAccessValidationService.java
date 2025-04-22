package com.example.fingerprint_backend.service;

import com.example.fingerprint_backend.model.access.Area;
import com.example.fingerprint_backend.model.access.AreaAccess;
import com.example.fingerprint_backend.model.auth.Employee;
import com.example.fingerprint_backend.repository.access.AreaAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AreaAccessValidationService {

    private final AreaAccessRepository areaAccessRepository;

    public boolean validateAccess(Employee employee, Area area) {
        if (employee == null || area == null) {
            return false;
        }

        return areaAccessRepository.existsByEmployeeAndArea(employee, area);
    }

    public boolean validateAccessById(String employeeId, String areaId) {
        if (employeeId == null || areaId == null) {
            return false;
        }

        // Find employee access permissions for the area
        Optional<AreaAccess> accessPermission =
                areaAccessRepository.findByEmployeeAndArea(
                        Employee.builder().id(employeeId).build(),
                        Area.builder().id(areaId).build());

        return accessPermission.isPresent();
    }

    public List<AreaAccess> getEmployeeAccessAreas(Employee employee) {
        return areaAccessRepository.findByEmployee(employee);
    }

    public List<AreaAccess> getEmployeeAccessAreasByEmployeeId(String employeeId) {
        return areaAccessRepository.findByEmployeeId(employeeId);
    }

    public List<AreaAccess> getAreaAccessEmployees(Area area) {
        return areaAccessRepository.findByArea(area);
    }

    public List<AreaAccess> getAreaAccessEmployeesByAreaId(String areaId) {
        return areaAccessRepository.findByAreaId(areaId);
    }
}
package com.example.fingerprint_backend.repository.access;

import com.example.fingerprint_backend.model.access.Area;
import com.example.fingerprint_backend.model.access.EmployeeAreaAccess;
import com.example.fingerprint_backend.model.auth.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface EmployeeAreaAccessRepository extends JpaRepository<EmployeeAreaAccess, String> {
    List<EmployeeAreaAccess> findByEmployee(Employee employee);
    List<EmployeeAreaAccess> findByEmployeeId(String employeeId);

    List<EmployeeAreaAccess> findByArea(Area area);
    List<EmployeeAreaAccess> findByAreaId(String areaId);

    Optional<EmployeeAreaAccess> findByEmployeeAndArea(Employee employee, Area area);

    boolean existsByEmployeeAndArea(Employee employee, Area area);

    @Transactional
    @Modifying
    @Query("DELETE FROM EmployeeAreaAccess e WHERE e.employee.id = ?1 AND e.area.id = ?2")
    int deleteByEmployeeIdAndAreaId(String employeeId, String areaId);
}

package com.example.fingerprint_backend.repository;

import com.example.fingerprint_backend.dto.EmployeeDTO;
import com.example.fingerprint_backend.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    @Query("SELECT new com.example.fingerprint_backend.dto.EmployeeDTO(e.id, e.fullName, e.phoneNumber, e.photo, e.address) FROM Employee e")
    List<EmployeeDTO> findAllEmployeesDTO();

    @Query("SELECT new com.example.fingerprint_backend.dto.EmployeeDTO(e.id, e.fullName, e.phoneNumber, e.photo, e.address) FROM Employee e WHERE e.id = :id")
    EmployeeDTO findByIdDTO(String id);
}
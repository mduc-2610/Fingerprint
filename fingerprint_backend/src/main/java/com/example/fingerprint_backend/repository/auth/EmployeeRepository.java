package com.example.fingerprint_backend.repository.auth;

import com.example.fingerprint_backend.model.auth.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    @Query("SELECT new Employee(e.id, e.fullName, e.phoneNumber, e.photo, e.address) FROM Employee e")
    List<Employee> findAllEmployees();

    @Query("SELECT new Employee(e.id, e.fullName, e.phoneNumber, e.photo, e.address) FROM Employee e WHERE e.id = :id")
    Optional<Employee> findById(String id);
}
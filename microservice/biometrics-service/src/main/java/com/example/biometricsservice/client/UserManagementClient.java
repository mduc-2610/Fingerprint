package com.example.biometricsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "user-management-service",
    url = "${user-management-service.url:http://localhost:8086}"
)
public interface UserManagementClient {

    @GetMapping("/api/employees/{id}")
    ResponseEntity<Object> getEmployeeById(@PathVariable String id);
}

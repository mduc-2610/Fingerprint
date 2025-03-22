package com.example.fingerprint_backend.dto;

import com.example.fingerprint_backend.model.Employee;
import lombok.Data;

@Data
public class EmployeeDTO {
    private String id;
    private String fullName;
    private String phoneNumber;
    private String photo;
    private String address;

    public EmployeeDTO(String id, String fullName, String phoneNumber, String photo, String address) {
        this.id = id;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.photo = photo;
        this.address = address;
    }
}

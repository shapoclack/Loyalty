package org.example.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Customer(
        int id,
        String fullName,
        String phone,
        String email,
        LocalDate birthDate,
        LocalDateTime createdAt,
        CustomerStatus status) {

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }
}

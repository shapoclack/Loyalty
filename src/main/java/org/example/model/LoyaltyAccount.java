package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoyaltyAccount(
        int id,
        int customerId,
        String cardNumber,
        BigDecimal balance,
        Tier tier,
        AccountStatus status,
        LocalDateTime openedAt,
        LocalDateTime updatedAt) {

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }
}

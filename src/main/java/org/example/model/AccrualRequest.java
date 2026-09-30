package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccrualRequest(
        int id,
        int accountId,
        int customerId,
        Integer ruleId,
        int operationId,
        BigDecimal bonusAmount,
        RequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime processedAt) {
}

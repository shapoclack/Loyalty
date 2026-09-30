package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RedemptionRequest(
        int id,
        int accountId,
        int customerId,
        int ruleId,
        int operationId,
        BigDecimal bonusAmount,
        RequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime processedAt) {
}

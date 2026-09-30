package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SourceOperation(
        int id,
        int customerId,
        OperationType type,
        String externalId,
        BigDecimal amount,
        LocalDateTime operationDate,
        String storeCode) {
}

package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Движение по бонусному счёту. {@code amount} со знаком: начисление > 0, списание < 0. */
public record BonusTransaction(
        int id,
        int accountId,
        Integer accrualRequestId,
        Integer redemptionRequestId,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        LocalDateTime createdAt) {
}

package org.example.model.query;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Фильтр журнала операций; null в поле означает «без ограничения». */
public record OperationFilter(LocalDate from, LocalDate to, String storeCode, BigDecimal minAmount) {
}

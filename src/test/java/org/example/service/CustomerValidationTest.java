package org.example.service;

import org.example.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class CustomerValidationTest {
    @Test void normalizesFormattedPhone() {
        assertEquals("+79991234567", CustomerValidation.phone("+7 (999) 123-45-67"));
    }
    @Test void rejectsInvalidCustomerFields() {
        assertThrows(BusinessException.class, () -> CustomerValidation.validate(" ", "+79991234567", null, null));
        assertThrows(BusinessException.class, () -> CustomerValidation.validate("И".repeat(201), "+79991234567", null, null));
        assertThrows(BusinessException.class, () -> CustomerValidation.validate("Иван", "+79991234567", "bad@", null));
        assertThrows(BusinessException.class, () -> CustomerValidation.validate("Иван", "+79991234567", null, LocalDate.now().plusDays(1)));
        assertThrows(BusinessException.class, () -> CustomerValidation.phone("++79991234567"));
        assertThrows(BusinessException.class, () -> CustomerValidation.phone("79991234567abc"));
        assertThrows(BusinessException.class, () -> CustomerValidation.phone("123"));
    }
    @Test void validatesPurchasePrecisionAndBounds() {
        assertEquals(new java.math.BigDecimal("1250.50"), PurchaseValidation.number("1250,50", "Сумма"));
        assertThrows(BusinessException.class, () -> PurchaseValidation.number("NaN", "Сумма"));
        assertThrows(BusinessException.class, () -> PurchaseValidation.validate(new java.math.BigDecimal("0"), null, null, null));
        assertThrows(BusinessException.class, () -> PurchaseValidation.validate(new java.math.BigDecimal("1.001"), null, null, null));
        assertThrows(BusinessException.class, () -> PurchaseValidation.validate(java.math.BigDecimal.TEN, new java.math.BigDecimal("-1"), null, null));
        assertThrows(BusinessException.class, () -> PurchaseValidation.validate(java.math.BigDecimal.TEN, null, "x".repeat(65), null));
    }
}

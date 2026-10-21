package org.example.service;

import org.example.exception.BusinessException;
import java.time.LocalDate;

/** Общая проверка для графического интерфейса и сервисов. */
public final class CustomerValidation {
    private CustomerValidation() { }

    public static void validate(String name, String phone, String email, LocalDate birthDate) {
        if (name == null || name.isBlank()) throw new BusinessException("ФИО: заполните обязательное поле");
        if (name.trim().length() > 200) throw new BusinessException("ФИО: максимум 200 символов");
        phone(phone);
        if (email != null && !email.isBlank()) {
            if (email.trim().length() > 200) throw new BusinessException("Email: максимум 200 символов");
            if (!email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
                throw new BusinessException("Email: укажите адрес в формате name@example.ru");
        }
        if (birthDate != null && (birthDate.isAfter(LocalDate.now())
                || birthDate.isBefore(LocalDate.now().minusYears(120))))
            throw new BusinessException("Дата рождения: укажите дату за последние 120 лет, не позже сегодняшней");
    }

    public static String phone(String value) {
        if (value == null || !value.matches("[+0-9()\\s-]+"))
            throw new BusinessException("Телефон: используйте цифры, пробелы, скобки, дефис и + в начале");
        String normalized = value.replaceAll("[()\\s-]", "");
        if (!normalized.matches("\\+?[0-9]{10,15}"))
            throw new BusinessException("Телефон: нужно от 10 до 15 цифр; + допускается только в начале");
        return normalized;
    }
}

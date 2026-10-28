package org.example.service;

import org.example.exception.BusinessException;
import java.time.LocalDate;
import java.util.regex.Pattern;

/** Общая проверка для графического интерфейса и сервисов. */
public final class CustomerValidation {
    public static final String NAME_FORMAT_MESSAGE = "ФИО: допускаются только буквы, разделённые пробелами или дефисами";
    private static final Pattern NAME_INPUT = Pattern.compile("[\\p{L}\\p{M} -]*");
    private static final Pattern NAME = Pattern.compile("\\p{L}[\\p{L}\\p{M}]*(?:[ -]+\\p{L}[\\p{L}\\p{M}]*)*");

    private CustomerValidation() { }

    /** Допускает промежуточный ввод и очистку поля; окончательное значение проверяется при сохранении. */
    public static boolean isNameInput(String value) {
        return value != null && NAME_INPUT.matcher(value).matches();
    }

    public static void validate(String name, String phone, String email, LocalDate birthDate) {
        if (name == null || name.isBlank()) throw new BusinessException("ФИО: заполните обязательное поле");
        if (name.trim().length() > 200) throw new BusinessException("ФИО: максимум 200 символов");
        if (!NAME.matcher(name.trim()).matches()) throw new BusinessException(NAME_FORMAT_MESSAGE);
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

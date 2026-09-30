package org.example.exception;

/** Запрошенная запись отсутствует в базе данных. */
public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}

package org.example.exception;

/** Нарушение бизнес-правила; сообщение показывается пользователю как есть. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}

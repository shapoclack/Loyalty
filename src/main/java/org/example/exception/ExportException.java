package org.example.exception;

/** Ошибка записи файла выгрузки. */
public class ExportException extends RuntimeException {

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}

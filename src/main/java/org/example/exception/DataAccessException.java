package org.example.db;

import java.sql.SQLException;

/** Непроверяемая обёртка над SQLException. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(SQLException cause) {
        super(cause.getMessage(), cause);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}

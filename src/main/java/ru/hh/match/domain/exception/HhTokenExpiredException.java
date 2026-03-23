package ru.hh.match.domain.exception;

public class HhTokenExpiredException extends RuntimeException {

    public HhTokenExpiredException(String message) {
        super(message);
    }

    public HhTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}

package ru.hh.match.domain.exception;

public class HhApiException extends RuntimeException {

    public HhApiException(String message) {
        super(message);
    }

    public HhApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

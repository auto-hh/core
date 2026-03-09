package ru.hh.match.domain.exception;

public class MatchingException extends RuntimeException {

    public MatchingException(String message) {
        super(message);
    }

    public MatchingException(String message, Throwable cause) {
        super(message, cause);
    }
}

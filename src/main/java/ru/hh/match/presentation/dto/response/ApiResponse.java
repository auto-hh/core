package ru.hh.match.presentation.dto.response;

import java.time.LocalDateTime;

public record ApiResponse<T>(
    boolean success,
    T data,
    String error,
    LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> fail(String error) {
        return new ApiResponse<>(false, null, error, LocalDateTime.now());
    }
}

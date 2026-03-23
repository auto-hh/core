package ru.hh.match.presentation.dto.response;

public record ResumeShortResponse(
    String id,
    String title,
    String status,
    String updatedAt,
    boolean isActive,
    String hhUrl
) {}

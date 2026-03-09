package ru.hh.match.presentation.dto.response;

public record MatchResultResponse(
    Long id,
    Long resumeId,
    Long vacancyId,
    String vacancyJobTitle,
    Double score,
    String status
) {}

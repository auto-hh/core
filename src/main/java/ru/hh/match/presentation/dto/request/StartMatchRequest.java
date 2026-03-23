package ru.hh.match.presentation.dto.request;

public record StartMatchRequest(
    String resumeId,
    String query
) {}

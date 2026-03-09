package ru.hh.match.infrastructure.adapter.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MatchResponseMessage(
    String correlationId,
    String sessionId,
    Long resumeId,
    Long vacancyId,
    Double score,
    String status
) {}

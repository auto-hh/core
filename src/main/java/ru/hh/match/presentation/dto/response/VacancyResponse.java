package ru.hh.match.presentation.dto.response;

public record VacancyResponse(
    Long id,
    String hhVacancyId,
    String targetRole,
    String jobTitle,
    String experience,
    String grade,
    String skillsVac,
    String vacancyText,
    String salary
) {}

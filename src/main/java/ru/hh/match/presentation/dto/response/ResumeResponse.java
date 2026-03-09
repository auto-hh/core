package ru.hh.match.presentation.dto.response;

public record ResumeResponse(
    Long id,
    String hhResumeId,
    String grade,
    String jobTitle,
    String location,
    Integer salaryVal,
    String salaryCurr,
    String skillsRes,
    String aboutMe,
    Integer expCount,
    String expText,
    String eduUni,
    String eduYear
) {}

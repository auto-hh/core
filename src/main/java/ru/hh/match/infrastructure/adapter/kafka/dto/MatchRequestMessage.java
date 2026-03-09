package ru.hh.match.infrastructure.adapter.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MatchRequestMessage(
    @JsonProperty("correlationId") String correlationId,
    @JsonProperty("sessionId") String sessionId,
    @JsonProperty("resumeId") Long resumeId,
    @JsonProperty("vacancyId") Long vacancyId,
    @JsonProperty("resume") ResumeData resume,
    @JsonProperty("vacancy") VacancyData vacancy
) {
    public record ResumeData(
        @JsonProperty("resume_id") Long resumeId,
        @JsonProperty("grade") String grade,
        @JsonProperty("job_title") String jobTitle,
        @JsonProperty("location") String location,
        @JsonProperty("salary_val") Integer salaryVal,
        @JsonProperty("salary_curr") String salaryCurr,
        @JsonProperty("skills_res") String skillsRes,
        @JsonProperty("about_me") String aboutMe,
        @JsonProperty("exp_count") Integer expCount,
        @JsonProperty("exp_text") String expText,
        @JsonProperty("edu_uni") String eduUni,
        @JsonProperty("edu_year") String eduYear
    ) {}

    public record VacancyData(
        @JsonProperty("vacancy_id") Long vacancyId,
        @JsonProperty("target_role") String targetRole,
        @JsonProperty("job_title") String jobTitle,
        @JsonProperty("experience") String experience,
        @JsonProperty("grade") String grade,
        @JsonProperty("skills_vac") String skillsVac,
        @JsonProperty("vacancy_text") String vacancyText,
        @JsonProperty("salary") String salary
    ) {}
}

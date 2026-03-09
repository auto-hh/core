package ru.hh.match.infrastructure.adapter.hh.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HhVacancyDto(
    String id,
    String name,
    String description,
    VacancyExperience experience,
    Salary salary,
    @JsonProperty("key_skills") List<KeySkill> keySkills,
    @JsonProperty("professional_roles") List<ProfessionalRole> professionalRoles
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VacancyExperience(String id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Salary(Integer from, Integer to, String currency) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KeySkill(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProfessionalRole(String name) {}
}

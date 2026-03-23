package ru.hh.match.infrastructure.adapter.hh.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HhResumeDto(
    String id,
    String title,
    @JsonProperty("alternate_url") String alternateUrl,
    Status status,
    Area area,
    Salary salary,
    @JsonProperty("skill_set") List<String> skillSet,
    String skills,
    @JsonProperty("total_experience") TotalExperience totalExperience,
    List<Experience> experience,
    Education education
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(String id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Area(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Salary(Integer amount, String currency) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TotalExperience(Integer months) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Experience(String position, String company) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Education(List<Primary> primary) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Primary(String name, Integer year) {}
    }
}

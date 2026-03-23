package ru.hh.match.infrastructure.adapter.hh.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HhMeDto(
    String id,
    @JsonProperty("first_name") String firstName,
    @JsonProperty("last_name") String lastName,
    @JsonProperty("middle_name") String middleName,
    String email,
    @JsonProperty("is_applicant") Boolean isApplicant,
    @JsonProperty("resumes_url") String resumesUrl,
    List<Resume> resumes
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resume(
        String id,
        String title,
        Status status,
        @JsonProperty("updated_at") String updatedAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(String id, String name) {}
}

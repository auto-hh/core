package ru.hh.match.infrastructure.adapter.hh.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HhResumeListDto(
    List<HhResumeListItem> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HhResumeListItem(String id) {}
}

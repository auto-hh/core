package ru.hh.match.infrastructure.adapter.hh.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HhVacancySearchDto(
    List<HhVacancyDto> items
) {}

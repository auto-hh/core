package ru.hh.match.application.port.out;

import java.util.List;
import ru.hh.match.infrastructure.adapter.hh.dto.HhVacancyDto;

public interface HhVacancyPort {

    List<HhVacancyDto> searchVacancies(String accessToken, String query, int perPage);

    List<HhVacancyDto> searchVacanciesPublic(String query, int perPage);
}

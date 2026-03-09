package ru.hh.match.application.port.in;

import java.util.List;
import java.util.UUID;
import ru.hh.match.domain.model.Vacancy;

public interface SearchVacanciesUseCase {

    List<Vacancy> searchVacancies(UUID sessionId);
}

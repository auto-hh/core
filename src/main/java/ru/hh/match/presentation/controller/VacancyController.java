package ru.hh.match.presentation.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hh.match.application.port.in.SearchVacanciesUseCase;
import ru.hh.match.domain.model.Vacancy;
import ru.hh.match.presentation.dto.response.ApiResponse;
import ru.hh.match.presentation.dto.response.VacancyResponse;

@RestController
@RequestMapping("/api/vacancies")
public class VacancyController {

    private final SearchVacanciesUseCase searchVacanciesUseCase;

    public VacancyController(SearchVacanciesUseCase searchVacanciesUseCase) {
        this.searchVacanciesUseCase = searchVacanciesUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VacancyResponse>>> searchVacancies(
            @CookieValue("session_id") UUID sessionId,
            @RequestParam(required = false) String query) {
        List<Vacancy> vacancies = (query != null && !query.isBlank())
                ? searchVacanciesUseCase.searchVacancies(sessionId, query)
                : searchVacanciesUseCase.searchVacancies(sessionId);
        List<VacancyResponse> responses = vacancies.stream()
                .map(v -> new VacancyResponse(
                        v.getId(),
                        v.getHhVacancyId(),
                        v.getTargetRole(),
                        v.getJobTitle(),
                        v.getExperience(),
                        v.getGrade(),
                        v.getSkillsVac(),
                        v.getVacancyText(),
                        v.getSalary()
                ))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }
}

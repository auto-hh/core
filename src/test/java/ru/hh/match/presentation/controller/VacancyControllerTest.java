package ru.hh.match.presentation.controller;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ru.hh.match.application.port.in.SearchVacanciesUseCase;
import ru.hh.match.domain.model.Vacancy;
import ru.hh.match.presentation.dto.response.VacancyResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VacancyControllerTest {

    @Mock private SearchVacanciesUseCase searchVacanciesUseCase;

    @InjectMocks private VacancyController vacancyController;

    @Test
    void searchVacancies_shouldReturnVacancyList() {
        UUID sessionId = UUID.randomUUID();
        Vacancy vacancy = new Vacancy();
        vacancy.setId(1L);
        vacancy.setHhVacancyId("v1");
        vacancy.setJobTitle("Senior Java Dev");

        when(searchVacanciesUseCase.searchVacancies(sessionId)).thenReturn(List.of(vacancy));

        var response = vacancyController.searchVacancies(sessionId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).hasSize(1);
        assertThat(response.getBody().data().getFirst().jobTitle()).isEqualTo("Senior Java Dev");
    }
}

package ru.hh.match.infrastructure.adapter.hh;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hh.match.infrastructure.adapter.hh.dto.HhVacancyDto;
import ru.hh.match.infrastructure.adapter.hh.dto.HhVacancySearchDto;
import ru.hh.match.infrastructure.config.AppProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HhVacancyAdapterTest {

    @Mock private HhApiClient hhApiClient;
    @Mock private AppProperties appProperties;

    @InjectMocks private HhVacancyAdapter hhVacancyAdapter;

    @Test
    void searchVacancies_shouldReturnVacancies() {
        var endpoints = new AppProperties.HhApi.Endpoints("/resumes/mine", "/vacancies", "/vacancies/{id}");
        var rateLimit = new AppProperties.HhApi.RateLimit(5, 3, 1000);
        var hhApi = new AppProperties.HhApi("https://api.hh.ru", "Test/1.0", rateLimit, endpoints);
        when(appProperties.hhApi()).thenReturn(hhApi);

        var vacancyDto = new HhVacancyDto("v1", "Java Dev", null, null, null, null, null);
        var searchResult = new HhVacancySearchDto(List.of(vacancyDto));
        when(hhApiClient.get(any(String.class), eq("token"), eq(HhVacancySearchDto.class)))
                .thenReturn(searchResult);

        List<HhVacancyDto> result = hhVacancyAdapter.searchVacancies("token", "Java", 50);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo("v1");
    }

    @Test
    void searchVacancies_nullResult_shouldReturnEmpty() {
        var endpoints = new AppProperties.HhApi.Endpoints("/resumes/mine", "/vacancies", "/vacancies/{id}");
        var rateLimit = new AppProperties.HhApi.RateLimit(5, 3, 1000);
        var hhApi = new AppProperties.HhApi("https://api.hh.ru", "Test/1.0", rateLimit, endpoints);
        when(appProperties.hhApi()).thenReturn(hhApi);

        when(hhApiClient.get(any(String.class), eq("token"), eq(HhVacancySearchDto.class)))
                .thenReturn(null);

        List<HhVacancyDto> result = hhVacancyAdapter.searchVacancies("token", "Java", 50);

        assertThat(result).isEmpty();
    }
}

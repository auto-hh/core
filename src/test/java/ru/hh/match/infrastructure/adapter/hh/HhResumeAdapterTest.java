package ru.hh.match.infrastructure.adapter.hh;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hh.match.domain.exception.ResumeNotFoundException;
import ru.hh.match.infrastructure.adapter.hh.dto.HhResumeDto;
import ru.hh.match.infrastructure.adapter.hh.dto.HhResumeListDto;
import ru.hh.match.infrastructure.config.AppProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HhResumeAdapterTest {

    @Mock private HhApiClient hhApiClient;
    @Mock private AppProperties appProperties;

    @InjectMocks private HhResumeAdapter hhResumeAdapter;

    @Test
    void fetchFirstResume_shouldReturnResume() {
        var endpoints = new AppProperties.HhApi.Endpoints("/resumes/mine", "/vacancies", "/vacancies/{id}");
        var rateLimit = new AppProperties.HhApi.RateLimit(5, 3, 1000);
        var hhApi = new AppProperties.HhApi("https://api.hh.ru", "Test/1.0", rateLimit, endpoints);
        when(appProperties.hhApi()).thenReturn(hhApi);

        var resumeList = new HhResumeListDto(List.of(new HhResumeListDto.HhResumeListItem("abc123")));
        when(hhApiClient.get(eq("/resumes/mine"), eq("token"), eq(HhResumeListDto.class)))
                .thenReturn(resumeList);

        var resumeDto = new HhResumeDto("abc123", "Java Dev", null, null, null, null, null, null, null);
        when(hhApiClient.get(eq("/resumes/abc123"), eq("token"), eq(HhResumeDto.class)))
                .thenReturn(resumeDto);

        HhResumeDto result = hhResumeAdapter.fetchFirstResume("token");

        assertThat(result.id()).isEqualTo("abc123");
        assertThat(result.title()).isEqualTo("Java Dev");
    }

    @Test
    void fetchFirstResume_emptyList_shouldThrow() {
        var endpoints = new AppProperties.HhApi.Endpoints("/resumes/mine", "/vacancies", "/vacancies/{id}");
        var rateLimit = new AppProperties.HhApi.RateLimit(5, 3, 1000);
        var hhApi = new AppProperties.HhApi("https://api.hh.ru", "Test/1.0", rateLimit, endpoints);
        when(appProperties.hhApi()).thenReturn(hhApi);

        when(hhApiClient.get(eq("/resumes/mine"), eq("token"), eq(HhResumeListDto.class)))
                .thenReturn(new HhResumeListDto(List.of()));

        assertThatThrownBy(() -> hhResumeAdapter.fetchFirstResume("token"))
                .isInstanceOf(ResumeNotFoundException.class);
    }
}

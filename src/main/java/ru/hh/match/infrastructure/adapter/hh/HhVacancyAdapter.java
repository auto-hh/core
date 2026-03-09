package ru.hh.match.infrastructure.adapter.hh;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import ru.hh.match.application.port.out.HhVacancyPort;
import ru.hh.match.infrastructure.adapter.hh.dto.HhVacancyDto;
import ru.hh.match.infrastructure.adapter.hh.dto.HhVacancySearchDto;
import ru.hh.match.infrastructure.config.AppProperties;

@Component
public class HhVacancyAdapter implements HhVacancyPort {

    private static final Logger log = LoggerFactory.getLogger(HhVacancyAdapter.class);

    private final HhApiClient hhApiClient;
    private final AppProperties appProperties;

    public HhVacancyAdapter(HhApiClient hhApiClient, AppProperties appProperties) {
        this.hhApiClient = hhApiClient;
        this.appProperties = appProperties;
    }

    @Override
    public List<HhVacancyDto> searchVacancies(String accessToken, String query, int perPage) {
        String path = UriComponentsBuilder.fromPath(appProperties.hhApi().endpoints().vacancySearch())
                .queryParam("text", query)
                .queryParam("per_page", perPage)
                .queryParam("order_by", "relevance")
                .build()
                .toUriString();

        log.info("Searching vacancies on HH API: {}", path);

        HhVacancySearchDto result = hhApiClient.get(path, accessToken, HhVacancySearchDto.class);

        if (result == null || result.items() == null) {
            return List.of();
        }

        log.info("Found {} vacancies from HH API", result.items().size());
        return result.items();
    }
}

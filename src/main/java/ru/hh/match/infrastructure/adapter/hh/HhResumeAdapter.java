package ru.hh.match.infrastructure.adapter.hh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.hh.match.application.port.out.HhResumePort;
import ru.hh.match.domain.exception.ResumeNotFoundException;
import ru.hh.match.infrastructure.adapter.hh.dto.HhResumeDto;
import ru.hh.match.infrastructure.adapter.hh.dto.HhResumeListDto;
import ru.hh.match.infrastructure.config.AppProperties;

@Component
public class HhResumeAdapter implements HhResumePort {

    private static final Logger log = LoggerFactory.getLogger(HhResumeAdapter.class);

    private final HhApiClient hhApiClient;
    private final AppProperties appProperties;

    public HhResumeAdapter(HhApiClient hhApiClient, AppProperties appProperties) {
        this.hhApiClient = hhApiClient;
        this.appProperties = appProperties;
    }

    @Override
    public HhResumeDto fetchFirstResume(String accessToken) {
        String resumesEndpoint = appProperties.hhApi().endpoints().resumes();
        log.info("Fetching resume list from HH API: {}", resumesEndpoint);

        HhResumeListDto resumeList = hhApiClient.get(resumesEndpoint, accessToken, HhResumeListDto.class);

        if (resumeList == null || resumeList.items() == null || resumeList.items().isEmpty()) {
            throw new ResumeNotFoundException("No resumes found on HH");
        }

        String resumeId = resumeList.items().getFirst().id();
        String resumeDetailPath = "/resumes/" + resumeId;
        log.info("Fetching full resume from HH API: {}", resumeDetailPath);

        return hhApiClient.get(resumeDetailPath, accessToken, HhResumeDto.class);
    }
}

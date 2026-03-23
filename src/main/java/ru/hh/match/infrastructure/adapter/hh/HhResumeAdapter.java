package ru.hh.match.infrastructure.adapter.hh;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.hh.match.application.port.out.HhResumePort;
import ru.hh.match.domain.exception.ResumeNotFoundException;
import ru.hh.match.infrastructure.adapter.hh.dto.HhMeDto;
import ru.hh.match.infrastructure.adapter.hh.dto.HhResumeDto;
import ru.hh.match.infrastructure.adapter.hh.dto.HhResumeListDto;

@Component
public class HhResumeAdapter implements HhResumePort {

    private static final Logger log = LoggerFactory.getLogger(HhResumeAdapter.class);

    private final HhApiClient hhApiClient;

    public HhResumeAdapter(HhApiClient hhApiClient) {
        this.hhApiClient = hhApiClient;
    }

    @Override
    public List<HhResumeDto> fetchAllResumes(String accessToken) {
        // Step 1: Get /me to get resumes_url
        log.info("Fetching /me from HH API");
        HhMeDto me = hhApiClient.get("/me", accessToken, HhMeDto.class);
        
        if (me.isApplicant() == null || !me.isApplicant()) {
            throw new ResumeNotFoundException("User account is not an applicant");
        }

        String resumesUrl = me.resumesUrl();
        if (resumesUrl == null || resumesUrl.isBlank()) {
            throw new ResumeNotFoundException("No resumes_url in /me response");
        }

        // Step 2: Get resume list from resumes_url
        log.info("Fetching resume list from: {}", resumesUrl);
        HhResumeListDto resumeList = hhApiClient.getAbsoluteUrl(resumesUrl, accessToken, HhResumeListDto.class);

        if (resumeList == null || resumeList.items() == null || resumeList.items().isEmpty()) {
            throw new ResumeNotFoundException("User has no resumes");
        }

        // Step 3: Fetch full details for each resume
        List<HhResumeDto> result = new ArrayList<>();
        for (HhResumeListDto.HhResumeListItem item : resumeList.items()) {
            String resumeUrl = item.url();
            if (resumeUrl == null || resumeUrl.isBlank()) {
                log.warn("Resume {} has no URL, skipping", item.id());
                continue;
            }

            log.info("Fetching full resume from: {}", resumeUrl);
            HhResumeDto fullResume = hhApiClient.getAbsoluteUrl(resumeUrl, accessToken, HhResumeDto.class);
            if (fullResume != null) {
                result.add(fullResume);
            }
        }

        if (result.isEmpty()) {
            throw new ResumeNotFoundException("Failed to fetch any resume details");
        }

        log.info("Fetched {} resumes from HH API", result.size());
        return result;
    }

    @Override
    public HhResumeDto fetchResumeById(String resumeId, String accessToken) {
        String resumePath = "/resumes/" + resumeId;
        log.info("Fetching resume by ID: {}", resumePath);
        return hhApiClient.get(resumePath, accessToken, HhResumeDto.class);
    }

    @Override
    public HhMeDto fetchMe(String accessToken) {
        log.info("Fetching /me from HH API");
        return hhApiClient.get("/me", accessToken, HhMeDto.class);
    }
}

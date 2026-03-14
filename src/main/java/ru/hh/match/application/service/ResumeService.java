package ru.hh.match.application.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.hh.match.application.port.in.SyncResumeUseCase;
import ru.hh.match.application.port.out.CachePort;
import ru.hh.match.application.port.out.HhResumePort;
import ru.hh.match.application.port.out.SessionPort;
import ru.hh.match.domain.exception.ResumeNotFoundException;
import ru.hh.match.domain.exception.SessionNotFoundException;
import ru.hh.match.domain.model.Resume;
import ru.hh.match.domain.repository.ResumeRepository;
import ru.hh.match.infrastructure.adapter.hh.dto.HhResumeDto;
import ru.hh.match.infrastructure.config.AppProperties;
import ru.hh.match.infrastructure.mapper.ResumeMapper;

@Service
public class ResumeService implements SyncResumeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final HhResumePort hhResumePort;
    private final SessionPort sessionPort;
    private final ResumeRepository resumeRepository;
    private final CachePort cachePort;
    private final ResumeMapper resumeMapper;
    private final AppProperties appProperties;
    private final JsonMapper objectMapper;

    public ResumeService(HhResumePort hhResumePort,
                         SessionPort sessionPort,
                         ResumeRepository resumeRepository,
                         CachePort cachePort,
                         ResumeMapper resumeMapper,
                         AppProperties appProperties,
                         JsonMapper objectMapper) {
        this.hhResumePort = hhResumePort;
        this.sessionPort = sessionPort;
        this.resumeRepository = resumeRepository;
        this.cachePort = cachePort;
        this.resumeMapper = resumeMapper;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Resume syncResume(UUID sessionId) {
        String accessToken = sessionPort.getAccessToken(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));

        boolean firstVisit = !cachePort.exists("session:seen:" + sessionId);

        if (firstVisit) {
            log.info("First visit for session {}, syncing resume from HH", maskSessionId(sessionId));
            HhResumeDto resumeDto = hhResumePort.fetchFirstResume(accessToken);
            if (resumeDto == null) {
                throw new ResumeNotFoundException("No resume found on HH for session: " + sessionId);
            }

            Resume resume = resumeMapper.toEntity(resumeDto);
            resume.setSessionId(sessionId);

            Resume saved = resumeRepository.findByHhResumeId(resume.getHhResumeId())
                    .map(existing -> {
                        updateExistingResume(existing, resume);
                        return resumeRepository.save(existing);
                    })
                    .orElseGet(() -> resumeRepository.save(resume));

            cacheResume(sessionId, saved);
            cachePort.set("session:seen:" + sessionId, "1", appProperties.cache().resumeTtl());

            log.info("Resume synced for session {}", maskSessionId(sessionId));
            return saved;
        }

        return getCachedOrDbResume(sessionId);
    }

    private Resume getCachedOrDbResume(UUID sessionId) {
        return cachePort.get("cache:resume:" + sessionId)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, Resume.class);
                    } catch (JacksonException e) {
                        log.warn("Failed to deserialize cached resume, falling back to DB", e);
                        return null;
                    }
                })
                .orElseGet(() -> resumeRepository.findBySessionId(sessionId)
                        .orElseThrow(() -> new ResumeNotFoundException("Resume not found for session: " + sessionId)));
    }

    private void cacheResume(UUID sessionId, Resume resume) {
        try {
            String json = objectMapper.writeValueAsString(resume);
            cachePort.set("cache:resume:" + sessionId, json, appProperties.cache().resumeTtl());
        } catch (JacksonException e) {
            log.warn("Failed to cache resume for session {}", maskSessionId(sessionId), e);
        }
    }

    private void updateExistingResume(Resume existing, Resume updated) {
        existing.setGrade(updated.getGrade());
        existing.setJobTitle(updated.getJobTitle());
        existing.setLocation(updated.getLocation());
        existing.setSalaryVal(updated.getSalaryVal());
        existing.setSalaryCurr(updated.getSalaryCurr());
        existing.setSkillsRes(updated.getSkillsRes());
        existing.setAboutMe(updated.getAboutMe());
        existing.setExpCount(updated.getExpCount());
        existing.setExpText(updated.getExpText());
        existing.setEduUni(updated.getEduUni());
        existing.setEduYear(updated.getEduYear());
        existing.setSessionId(updated.getSessionId());
    }

    private String maskSessionId(UUID sessionId) {
        String s = sessionId.toString();
        return s.substring(0, s.length() - 4) + "****";
    }
}

package ru.hh.match.application.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
public class ResumeService {

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

    public List<Resume> getAllResumes(UUID sessionId) {
        // Check cache first
        String cacheKey = "cache:resumes:" + sessionId;
        var cached = cachePort.get(cacheKey);
        if (cached.isPresent()) {
            try {
                return objectMapper.readValue(cached.get(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Resume.class));
            } catch (JacksonException e) {
                log.warn("Failed to deserialize cached resumes, falling back to DB", e);
            }
        }

        // Check DB
        List<Resume> fromDb = resumeRepository.findAllBySessionId(sessionId);
        if (!fromDb.isEmpty()) {
            cacheResumes(sessionId, fromDb);
            return fromDb;
        }

        // Sync from HH
        return syncAllResumes(sessionId);
    }

    @Transactional
    public List<Resume> syncAllResumes(UUID sessionId) {
        String accessToken = sessionPort.getAccessToken(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));

        log.info("Syncing all resumes for session {}", maskSessionId(sessionId));
        List<HhResumeDto> dtos = hhResumePort.fetchAllResumes(accessToken);

        List<Resume> savedList = new ArrayList<>();
        for (HhResumeDto dto : dtos) {
            Resume resume = resumeRepository.findByHhResumeId(dto.id())
                    .orElse(new Resume());
            
            resumeMapper.updateFromDto(dto, resume);
            resume.setSessionId(sessionId);
            savedList.add(resumeRepository.save(resume));
        }

        // If no active resume, make first one active
        boolean hasActive = savedList.stream().anyMatch(Resume::isActive);
        if (!hasActive && !savedList.isEmpty()) {
            Resume first = savedList.get(0);
            first.setActive(true);
            resumeRepository.save(first);
        }

        cacheResumes(sessionId, savedList);
        cachePort.set("session:seen:" + sessionId, "1", appProperties.cache().resumeTtl());

        log.info("Synced {} resumes for session {}", savedList.size(), maskSessionId(sessionId));
        return savedList;
    }

    public Resume getResume(UUID sessionId, Long resumeId) {
        return resumeRepository.findById(resumeId)
                .filter(r -> r.getSessionId().equals(sessionId))
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found: " + resumeId));
    }

    public Resume getActiveResume(UUID sessionId) {
        return resumeRepository.findBySessionIdAndIsActiveTrue(sessionId)
                .orElseThrow(() -> new ResumeNotFoundException("No active resume for session"));
    }

    @Transactional
    public void activateResume(UUID sessionId, Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .filter(r -> r.getSessionId().equals(sessionId))
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found: " + resumeId));

        // Deactivate all resumes for this session
        resumeRepository.deactivateAllForSession(sessionId);

        // Activate selected resume
        resume.setActive(true);
        resumeRepository.save(resume);

        // Invalidate cache
        cachePort.delete("cache:resumes:" + sessionId);

        log.info("Activated resume {} for session {}", resumeId, maskSessionId(sessionId));
    }

    @Transactional
    public Resume syncResume(UUID sessionId, String hhResumeId) {
        String accessToken = sessionPort.getAccessToken(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));

        log.info("Syncing single resume {} for session {}", hhResumeId, maskSessionId(sessionId));
        HhResumeDto dto = hhResumePort.fetchResumeById(hhResumeId, accessToken);
        
        Resume resume = resumeRepository.findByHhResumeId(dto.id())
                .orElse(new Resume());
        
        resumeMapper.updateFromDto(dto, resume);
        resume.setSessionId(sessionId);
        Resume saved = resumeRepository.save(resume);

        // Invalidate cache
        cachePort.delete("cache:resumes:" + sessionId);

        return saved;
    }

    private void cacheResumes(UUID sessionId, List<Resume> resumes) {
        try {
            String json = objectMapper.writeValueAsString(resumes);
            cachePort.set("cache:resumes:" + sessionId, json, appProperties.cache().resumeTtl());
        } catch (JacksonException e) {
            log.warn("Failed to cache resumes for session {}", maskSessionId(sessionId), e);
        }
    }

    private String maskSessionId(UUID sessionId) {
        String s = sessionId.toString();
        return s.substring(0, s.length() - 4) + "****";
    }
}

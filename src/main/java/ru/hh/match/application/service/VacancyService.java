package ru.hh.match.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.hh.match.application.port.in.SearchVacanciesUseCase;
import ru.hh.match.application.port.out.CachePort;
import ru.hh.match.application.port.out.HhVacancyPort;
import ru.hh.match.application.port.out.SessionPort;
import ru.hh.match.domain.exception.ResumeNotFoundException;
import ru.hh.match.domain.exception.SessionNotFoundException;
import ru.hh.match.domain.model.Resume;
import ru.hh.match.domain.model.Vacancy;
import ru.hh.match.domain.repository.ResumeRepository;
import ru.hh.match.domain.repository.VacancyRepository;
import ru.hh.match.infrastructure.adapter.hh.dto.HhVacancyDto;
import ru.hh.match.infrastructure.config.AppProperties;
import ru.hh.match.infrastructure.mapper.VacancyMapper;

@Service
public class VacancyService implements SearchVacanciesUseCase {

    private static final Logger log = LoggerFactory.getLogger(VacancyService.class);

    private final HhVacancyPort hhVacancyPort;
    private final SessionPort sessionPort;
    private final ResumeRepository resumeRepository;
    private final VacancyRepository vacancyRepository;
    private final VacancyMapper vacancyMapper;
    private final AppProperties appProperties;
    private final CachePort cachePort;

    public VacancyService(HhVacancyPort hhVacancyPort,
                          SessionPort sessionPort,
                          ResumeRepository resumeRepository,
                          VacancyRepository vacancyRepository,
                          VacancyMapper vacancyMapper,
                          AppProperties appProperties,
                          CachePort cachePort) {
        this.hhVacancyPort = hhVacancyPort;
        this.sessionPort = sessionPort;
        this.resumeRepository = resumeRepository;
        this.vacancyRepository = vacancyRepository;
        this.vacancyMapper = vacancyMapper;
        this.appProperties = appProperties;
        this.cachePort = cachePort;
    }

    @Override
    public List<Vacancy> searchVacancies(UUID sessionId) {
        String accessToken = sessionPort.getAccessToken(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));

        Resume resume = resumeRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found for session: " + sessionId));

        String query = buildSearchQuery(resume);
        int limit = appProperties.vacancy().searchLimit();

        log.info("Searching vacancies for session {}, query: {}", maskSessionId(sessionId), query);

        List<HhVacancyDto> hhVacancies = hhVacancyPort.searchVacancies(accessToken, query, limit);

        List<Vacancy> result = new ArrayList<>();
        for (HhVacancyDto dto : hhVacancies) {
            Vacancy vacancy = vacancyRepository.findByHhVacancyId(dto.id())
                    .orElseGet(() -> {
                        Vacancy newVacancy = vacancyMapper.toEntity(dto);
                        return vacancyRepository.save(newVacancy);
                    });
            result.add(vacancy);
        }

        log.info("Found {} vacancies for session {}", result.size(), maskSessionId(sessionId));
        return result;
    }

    private String buildSearchQuery(Resume resume) {
        StringBuilder query = new StringBuilder();
        if (resume.getJobTitle() != null && !resume.getJobTitle().isEmpty()) {
            query.append(resume.getJobTitle());
        }
        if (resume.getSkillsRes() != null && !resume.getSkillsRes().isEmpty()) {
            if (!query.isEmpty()) {
                query.append(" ");
            }
            query.append(resume.getSkillsRes());
        }
        return query.toString();
    }

    private String maskSessionId(UUID sessionId) {
        String s = sessionId.toString();
        return s.substring(0, s.length() - 4) + "****";
    }
}

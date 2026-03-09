package ru.hh.match.application.service;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.hh.match.application.port.in.StartMatchingUseCase;
import ru.hh.match.application.port.out.MatchRequestPort;
import ru.hh.match.application.port.out.SessionPort;
import ru.hh.match.domain.exception.MatchingException;
import ru.hh.match.domain.exception.ResumeNotFoundException;
import ru.hh.match.domain.exception.SessionNotFoundException;
import ru.hh.match.domain.model.MatchResult;
import ru.hh.match.domain.model.Resume;
import ru.hh.match.domain.model.Vacancy;
import ru.hh.match.domain.model.enums.MatchStatus;
import ru.hh.match.domain.repository.MatchResultRepository;
import ru.hh.match.domain.repository.ResumeRepository;
import ru.hh.match.domain.repository.VacancyRepository;
import ru.hh.match.infrastructure.adapter.kafka.dto.MatchRequestMessage;

@Service
public class MatchingService implements StartMatchingUseCase {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final SessionPort sessionPort;
    private final ResumeRepository resumeRepository;
    private final VacancyRepository vacancyRepository;
    private final MatchResultRepository matchResultRepository;
    private final MatchRequestPort matchRequestPort;

    public MatchingService(SessionPort sessionPort,
                           ResumeRepository resumeRepository,
                           VacancyRepository vacancyRepository,
                           MatchResultRepository matchResultRepository,
                           MatchRequestPort matchRequestPort) {
        this.sessionPort = sessionPort;
        this.resumeRepository = resumeRepository;
        this.vacancyRepository = vacancyRepository;
        this.matchResultRepository = matchResultRepository;
        this.matchRequestPort = matchRequestPort;
    }

    @Override
    public int startMatching(UUID sessionId) {
        sessionPort.getAccessToken(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found: " + sessionId));

        Resume resume = resumeRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResumeNotFoundException("Resume not found for session: " + sessionId));

        List<Vacancy> vacancies = vacancyRepository.findAll();

        int sentCount = 0;
        for (Vacancy vacancy : vacancies) {
            try {
                var existingMatch = matchResultRepository.findByResumeIdAndVacancyId(resume.getId(), vacancy.getId());

                if (existingMatch.isPresent() && existingMatch.get().getStatus() == MatchStatus.COMPLETED) {
                    continue;
                }

                MatchResult matchResult = existingMatch.orElseGet(() -> {
                    MatchResult mr = new MatchResult();
                    mr.setResume(resume);
                    mr.setVacancy(vacancy);
                    mr.setSessionId(sessionId);
                    return mr;
                });

                matchResult.setStatus(MatchStatus.PENDING);
                matchResult.setScore(null);
                matchResultRepository.save(matchResult);

                MatchRequestMessage message = buildMatchRequest(sessionId, resume, vacancy);
                matchRequestPort.sendMatchRequest(message);
                sentCount++;
            } catch (Exception e) {
                log.error("Failed to send match request for resume {} and vacancy {}",
                        resume.getId(), vacancy.getId(), e);
                throw new MatchingException("Failed to start matching", e);
            }
        }

        log.info("Matching started for session {}, sent {} requests", maskSessionId(sessionId), sentCount);
        return sentCount;
    }

    private MatchRequestMessage buildMatchRequest(UUID sessionId, Resume resume, Vacancy vacancy) {
        return new MatchRequestMessage(
                UUID.randomUUID().toString(),
                sessionId.toString(),
                resume.getId(),
                vacancy.getId(),
                new MatchRequestMessage.ResumeData(
                        resume.getId(),
                        resume.getGrade(),
                        resume.getJobTitle(),
                        resume.getLocation(),
                        resume.getSalaryVal(),
                        resume.getSalaryCurr(),
                        resume.getSkillsRes(),
                        resume.getAboutMe(),
                        resume.getExpCount(),
                        resume.getExpText(),
                        resume.getEduUni(),
                        resume.getEduYear()
                ),
                new MatchRequestMessage.VacancyData(
                        vacancy.getId(),
                        vacancy.getTargetRole(),
                        vacancy.getJobTitle(),
                        vacancy.getExperience(),
                        vacancy.getGrade(),
                        vacancy.getSkillsVac(),
                        vacancy.getVacancyText(),
                        vacancy.getSalary()
                )
        );
    }

    private String maskSessionId(UUID sessionId) {
        String s = sessionId.toString();
        return s.substring(0, s.length() - 4) + "****";
    }
}

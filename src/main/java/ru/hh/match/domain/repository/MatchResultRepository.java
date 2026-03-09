package ru.hh.match.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hh.match.domain.model.MatchResult;
import ru.hh.match.domain.model.enums.MatchStatus;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    Optional<MatchResult> findByResumeIdAndVacancyId(Long resumeId, Long vacancyId);

    List<MatchResult> findBySessionId(UUID sessionId);

    List<MatchResult> findBySessionIdAndStatus(UUID sessionId, MatchStatus status);
}

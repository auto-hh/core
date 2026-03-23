package ru.hh.match.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hh.match.domain.model.Resume;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByHhResumeId(String hhResumeId);

    Optional<Resume> findBySessionId(UUID sessionId);

    List<Resume> findAllBySessionId(UUID sessionId);

    Optional<Resume> findBySessionIdAndIsActiveTrue(UUID sessionId);

    @Modifying
    @Query("UPDATE Resume r SET r.isActive = false WHERE r.sessionId = :sessionId")
    void deactivateAllForSession(@Param("sessionId") UUID sessionId);
}

package ru.hh.match.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hh.match.domain.model.Resume;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByHhResumeId(String hhResumeId);

    Optional<Resume> findBySessionId(UUID sessionId);
}

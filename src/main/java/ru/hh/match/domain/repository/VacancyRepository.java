package ru.hh.match.domain.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.hh.match.domain.model.Vacancy;

public interface VacancyRepository extends JpaRepository<Vacancy, Long> {

    Optional<Vacancy> findByHhVacancyId(String hhVacancyId);
}

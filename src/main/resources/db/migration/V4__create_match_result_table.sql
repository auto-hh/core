CREATE TABLE matching.match_results (
    id              BIGSERIAL PRIMARY KEY,
    resume_id       BIGINT       NOT NULL,
    vacancy_id      BIGINT       NOT NULL,
    session_id      UUID         NOT NULL,
    score           DOUBLE PRECISION,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_match_resume  FOREIGN KEY (resume_id)  REFERENCES resume.resumes(id),
    CONSTRAINT fk_match_vacancy FOREIGN KEY (vacancy_id) REFERENCES vacancy.vacancies(id),
    CONSTRAINT uq_resume_vacancy UNIQUE (resume_id, vacancy_id)
);

CREATE INDEX idx_match_results_session_id ON matching.match_results(session_id);
CREATE INDEX idx_match_results_status ON matching.match_results(status);

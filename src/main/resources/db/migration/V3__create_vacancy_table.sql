CREATE TABLE vacancy.vacancies (
    id              BIGSERIAL PRIMARY KEY,
    hh_vacancy_id   VARCHAR(64)  NOT NULL UNIQUE,
    target_role     VARCHAR(512) NOT NULL DEFAULT '',
    job_title       VARCHAR(512) NOT NULL DEFAULT '',
    experience      VARCHAR(256) NOT NULL DEFAULT '',
    grade           VARCHAR(64)  NOT NULL DEFAULT '',
    skills_vac      TEXT         NOT NULL DEFAULT '',
    vacancy_text    TEXT         NOT NULL DEFAULT '',
    salary          VARCHAR(256) NOT NULL DEFAULT '',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vacancies_hh_vacancy_id ON vacancy.vacancies(hh_vacancy_id);

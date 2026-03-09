CREATE TABLE resume.resumes (
    id              BIGSERIAL PRIMARY KEY,
    hh_resume_id    VARCHAR(64)  NOT NULL UNIQUE,
    session_id      UUID         NOT NULL,
    grade           VARCHAR(64)  NOT NULL DEFAULT '',
    job_title       VARCHAR(512) NOT NULL DEFAULT '',
    location        VARCHAR(256) NOT NULL DEFAULT '',
    salary_val      INTEGER      NOT NULL DEFAULT 0,
    salary_curr     VARCHAR(16)  NOT NULL DEFAULT 'RUB',
    skills_res      TEXT         NOT NULL DEFAULT '',
    about_me        TEXT         NOT NULL DEFAULT '',
    exp_count       INTEGER      NOT NULL DEFAULT 0,
    exp_text        TEXT         NOT NULL DEFAULT '',
    edu_uni         VARCHAR(512) NOT NULL DEFAULT '',
    edu_year        VARCHAR(32)  NOT NULL DEFAULT '',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resumes_session_id ON resume.resumes(session_id);
CREATE INDEX idx_resumes_hh_resume_id ON resume.resumes(hh_resume_id);

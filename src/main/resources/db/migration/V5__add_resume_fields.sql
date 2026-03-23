ALTER TABLE resume.resumes
    ADD COLUMN IF NOT EXISTS status    VARCHAR(32)  DEFAULT 'published',
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN      DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS hh_url    VARCHAR(512) DEFAULT '';

-- Fix any NULL values from previous runs
UPDATE resume.resumes SET status = 'published' WHERE status IS NULL;
UPDATE resume.resumes SET is_active = FALSE WHERE is_active IS NULL;
UPDATE resume.resumes SET hh_url = '' WHERE hh_url IS NULL;

-- Now add NOT NULL constraints
ALTER TABLE resume.resumes ALTER COLUMN status SET NOT NULL;
ALTER TABLE resume.resumes ALTER COLUMN is_active SET NOT NULL;
ALTER TABLE resume.resumes ALTER COLUMN hh_url SET NOT NULL;

-- Ensure only one active resume per session
CREATE UNIQUE INDEX IF NOT EXISTS idx_resumes_active_per_session
    ON resume.resumes (session_id)
    WHERE is_active = TRUE;

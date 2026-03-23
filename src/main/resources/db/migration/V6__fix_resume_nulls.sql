-- Fix NULL values in existing rows
UPDATE resume.resumes SET status = 'published' WHERE status IS NULL;
UPDATE resume.resumes SET is_active = FALSE WHERE is_active IS NULL;
UPDATE resume.resumes SET hh_url = '' WHERE hh_url IS NULL;

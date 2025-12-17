-- Scheduled Jobs table
CREATE TABLE IF NOT EXISTS scheduled_jobs (
    id UUID PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    records_processed BIGINT,
    records_failed BIGINT,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_scheduled_jobs_type ON scheduled_jobs (job_type);
CREATE INDEX IF NOT EXISTS idx_scheduled_jobs_status ON scheduled_jobs (status);
CREATE INDEX IF NOT EXISTS idx_scheduled_jobs_created_at ON scheduled_jobs (created_at);

-- Job execution history for detailed tracking
CREATE TABLE IF NOT EXISTS job_execution_details (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES scheduled_jobs(id),
    step_name VARCHAR(100) NOT NULL,
    step_status VARCHAR(20) NOT NULL,
    records_in_step BIGINT,
    step_started_at TIMESTAMPTZ,
    step_completed_at TIMESTAMPTZ,
    error_details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_job_execution_job_id ON job_execution_details (job_id);

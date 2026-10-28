-- Preserve V21 questionnaire tasks while allowing canonical category combinations.
ALTER TABLE export_jobs DROP CONSTRAINT ck_export_types;
ALTER TABLE export_jobs ADD CONSTRAINT ck_export_types
    CHECK (data_types IN ('QUESTIONNAIRE', 'GAZE', 'GAZE,QUESTIONNAIRE'));

-- Match deterministic event export ordering without an application-side sort.
CREATE INDEX idx_gaze_events_session_elapsed
    ON gaze_events(session_id, session_elapsed_ms, batch_sequence, event_index);

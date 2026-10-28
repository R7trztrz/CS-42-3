-- Preserve historical selections and allow all nonempty canonical category combinations.
ALTER TABLE export_jobs DROP CONSTRAINT ck_export_types;
ALTER TABLE export_jobs ADD CONSTRAINT ck_export_types CHECK (data_types IN (
    'BEHAVIOR', 'GAZE', 'QUESTIONNAIRE', 'BEHAVIOR,GAZE', 'BEHAVIOR,QUESTIONNAIRE',
    'GAZE,QUESTIONNAIRE', 'BEHAVIOR,GAZE,QUESTIONNAIRE'
));

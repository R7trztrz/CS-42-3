-- Runtime settings are editable only while the study is a draft.
-- Existing studies and new studies start with both optional features disabled.
ALTER TABLE studies
    ADD COLUMN eye_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN questionnaire_enabled BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN studies.eye_tracking_enabled IS 'Enable eye-tracking calibration and collection when published.';
COMMENT ON COLUMN studies.questionnaire_enabled IS 'Present a questionnaire after manual browsing completion when published.';

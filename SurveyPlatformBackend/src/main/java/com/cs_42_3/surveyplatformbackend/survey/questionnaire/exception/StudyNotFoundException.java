package com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception;

import java.util.UUID;

/** Hides whether a study is absent or owned by another researcher. */
public class StudyNotFoundException extends RuntimeException {
    public StudyNotFoundException(UUID studyId) {
        super("Study not found: " + studyId);
    }
}

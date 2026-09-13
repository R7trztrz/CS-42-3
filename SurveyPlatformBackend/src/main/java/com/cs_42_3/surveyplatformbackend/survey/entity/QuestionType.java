package com.cs_42_3.surveyplatformbackend.survey.entity;

/**
 * The v1 question type set, per the requirements doc: single/multi-choice, scale, and open text.
 * SINGLE_CHOICE and MULTI_CHOICE share the same option structure (QuestionOption) and only
 * differ in how the participant-facing UI renders them (radio vs checkbox) — that rendering
 * decision belongs to M5, not this module.
 */
public enum QuestionType {
    SINGLE_CHOICE,
    MULTI_CHOICE,
    SCALE,
    TEXT
}

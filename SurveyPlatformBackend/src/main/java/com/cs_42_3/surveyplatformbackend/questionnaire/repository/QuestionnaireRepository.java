package com.cs_42_3.surveyplatformbackend.questionnaire.repository;

import com.cs_42_3.surveyplatformbackend.questionnaire.domain.Questionnaire;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence operations for questionnaires.
 */
public interface QuestionnaireRepository extends JpaRepository<Questionnaire, UUID> {

    /**
     * Finds the questionnaire owned by one study, if a draft has been saved.
     *
     * @param studyId the owning study
     * @return the study's questionnaire, or an empty result
     */
    Optional<Questionnaire> findByStudyId(UUID studyId);
}

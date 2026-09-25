package com.cs_42_3.surveyplatformbackend.survey.questionnaire.repository;

import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.Questionnaire;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Persistence access for study questionnaires and their ordered items. */
public interface QuestionnaireRepository extends JpaRepository<Questionnaire, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<Questionnaire> findByStudyId(UUID studyId);
}

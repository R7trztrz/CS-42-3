package com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.repository;

import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Read-once/write-once persistence for questionnaire publications. */
public interface QuestionnaireSnapshotRepository extends JpaRepository<QuestionnaireSnapshot, UUID> {
    Optional<QuestionnaireSnapshot> findByStudyId(UUID studyId);

    boolean existsByStudyId(UUID studyId);
}

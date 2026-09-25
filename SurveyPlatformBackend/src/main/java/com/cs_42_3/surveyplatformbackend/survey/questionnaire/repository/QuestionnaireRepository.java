package com.cs_42_3.surveyplatformbackend.survey.questionnaire.repository;

import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.Questionnaire;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Persistence access for study questionnaires and their ordered items. */
public interface QuestionnaireRepository extends JpaRepository<Questionnaire, UUID> {

    @EntityGraph(attributePaths = {"items", "items.branchRules", "items.branchRules.targetItem"})
    Optional<Questionnaire> findByStudyId(UUID studyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Questionnaire q where q.studyId = :studyId")
    Optional<Questionnaire> findByStudyIdForUpdate(@Param("studyId") UUID studyId);

    @Query("""
            select distinct q.studyId
            from Questionnaire q
            join q.items item
            where item.questionId = :questionId
            order by q.studyId
            """)
    List<UUID> findReferencedStudyIds(@Param("questionId") UUID questionId);

    @Query("""
            select distinct q.id
            from Questionnaire q
            join q.items item
            where item.questionId = :questionId
            order by q.id
            """)
    List<UUID> findIdsReferencingQuestion(@Param("questionId") UUID questionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select q
            from Questionnaire q
            where q.id in :questionnaireIds
            order by q.id
            """)
    List<Questionnaire> lockAllByIds(
            @Param("questionnaireIds") Collection<UUID> questionnaireIds
    );

    @EntityGraph(attributePaths = {"items", "items.branchRules", "items.branchRules.targetItem"})
    @Query("""
            select distinct q
            from Questionnaire q
            join q.items item
            where item.questionId = :questionId
            """)
    List<Questionnaire> findAllWithGraphReferencingQuestion(
            @Param("questionId") UUID questionId
    );
}

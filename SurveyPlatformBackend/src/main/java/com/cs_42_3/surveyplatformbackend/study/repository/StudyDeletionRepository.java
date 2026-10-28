package com.cs_42_3.surveyplatformbackend.study.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Removes restrictive dependencies while retaining shared templates and question-bank entries.
 * The caller must hold the Study write lock and delete the aggregate in the same transaction.
 *
 * @author Simon Tian
 */
@Repository
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class StudyDeletionRepository {
    private final JdbcTemplate jdbc;

    public void removeDependencies(UUID studyId, Instant now) {
        // Wait for active generators before queuing deterministic ZIP/temporary paths.
        // Generators never acquire a Study lock, so this preserves the Study-first lock order.
        jdbc.query("SELECT id FROM export_jobs WHERE study_id = ? ORDER BY id FOR UPDATE",
                (rs, row) -> rs.getObject(1, UUID.class), studyId);
        jdbc.update("""
                INSERT INTO export_file_cleanup_tasks(export_id, created_at, next_attempt_at)
                SELECT id, ?, ? FROM export_jobs WHERE study_id = ?
                ON CONFLICT (export_id) DO NOTHING
                """, Timestamp.from(now), Timestamp.from(now), studyId);
        // Immutable asset IDs also identify tasks; no files are touched inside this transaction.
        jdbc.update("""
                INSERT INTO asset_file_cleanup_tasks
                    (id, storage_key, created_at, next_attempt_at)
                SELECT id, storage_key, ?, ? FROM study_assets WHERE study_id = ?
                """, Timestamp.from(now), Timestamp.from(now), studyId);

        // Delete source rules first because their target item references use RESTRICT.
        // A corrupt cross-study reference must fail rather than silently alter another study.
        jdbc.update("""
                DELETE FROM questionnaire_branch_rules WHERE source_item_id IN (
                    SELECT i.id FROM questionnaire_items i JOIN questionnaires q
                    ON q.id = i.questionnaire_id WHERE q.study_id = ?)
                """, studyId);
        jdbc.update("DELETE FROM questionnaire_publication_snapshots WHERE study_id = ?", studyId);
        jdbc.update("DELETE FROM study_assets WHERE study_id = ?", studyId);
        // Study deletion cascades to feeds, questionnaires, items, sessions, steps and answers.
        // Future study-owned collection tables must join this contract through FKs or explicit cleanup.
    }
}

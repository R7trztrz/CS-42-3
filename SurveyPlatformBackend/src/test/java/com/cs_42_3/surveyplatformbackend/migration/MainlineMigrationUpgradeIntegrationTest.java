package com.cs_42_3.surveyplatformbackend.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Upgrade real PostgreSQL main V9/V10 baselines without rewriting applied history. */
@Testcontainers
class MainlineMigrationUpgradeIntegrationTest {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @ParameterizedTest
    @ValueSource(strings = {"9", "10"})
    void mainBaselineUpgradesToM4M5WithoutChangingHistoryOrExistingData(String baseline) throws Exception {
        String schema = "main_upgrade_" + baseline;
        flyway(schema, baseline).migrate();

        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            connection.setSchema(schema);
            UUID researcherId = UUID.randomUUID();
            UUID studyId = UUID.randomUUID();
            try (var insert = connection.prepareStatement(
                    "INSERT INTO researchers (id,email,password_hash) VALUES (?,?,'test-only-hash')")) {
                insert.setObject(1, researcherId);
                insert.setString(2, researcherId + "@migration.invalid");
                insert.executeUpdate();
            }
            try (var insert = connection.prepareStatement(
                    "INSERT INTO studies (id,owner_id,title,description) VALUES (?,?,'Existing main study','Keep this data')")) {
                insert.setObject(1, studyId);
                insert.setObject(2, researcherId);
                insert.executeUpdate();
            }
            try (var insert = connection.prepareStatement(
                    "INSERT INTO study_feeds (study_id,template_code,content,schema_version) VALUES (?,'facebook','{\"original\":true}',1)")) {
                insert.setObject(1, studyId);
                insert.executeUpdate();
            }
            String businessDigestSql = """
                    SELECT md5(row_to_json(r)::text || row_to_json(s)::text || row_to_json(f)::text)
                    FROM researchers r
                    JOIN (SELECT id,owner_id,title,description,status,created_at,updated_at,lock_version,
                                 eye_tracking_enabled,questionnaire_enabled,participation_token,published_at
                          FROM studies) s ON s.owner_id=r.id
                    JOIN study_feeds f ON f.study_id=s.id
                    """;
            List<String> originalHistory = values(connection,
                    "SELECT version || ':' || script || ':' || checksum || ':' || success "
                    + "FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank");
            List<String> originalBusinessData = values(connection, businessDigestSql);
            String originalTemplateScript = "V10__sync_all_platform_feed_templates.sql";
            if (baseline.equals("10")) {
                assertThat(originalHistory.get(9)).isEqualTo("10:" + originalTemplateScript + ":207355337:true");
            }

            var upgraded = flyway(schema, null);
            assertThat(upgraded.migrate().migrationsExecuted).isEqualTo(16 - Integer.parseInt(baseline));
            assertThat(upgraded.validateWithResult().validationSuccessful).isTrue();
            List<String> finalHistory = values(connection,
                    "SELECT version || ':' || script || ':' || checksum || ':' || success "
                    + "FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank");
            assertThat(finalHistory).hasSize(16).startsWith(originalHistory.toArray(String[]::new));
            assertThat(values(connection, businessDigestSql)).isEqualTo(originalBusinessData);
            assertThat(values(connection,
                    "SELECT script FROM flyway_schema_history WHERE version IN ('10','11','16') ORDER BY installed_rank"))
                    .containsExactly(originalTemplateScript, "V11__create_question_bank_tables.sql",
                            "V16__create_participant_questionnaire_runtime.sql");
            assertThat(values(connection,
                    "SELECT code FROM feed_templates WHERE content IS NOT NULL AND schema_version=1 ORDER BY code"))
                    .containsExactly("bluesky", "facebook", "instagram", "threads", "tiktok", "truth-social", "x");
            assertThat(values(connection,
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema='" + schema + "' "
                    + "AND table_name IN ('questions','question_options','questionnaires','questionnaire_items',"
                    + "'questionnaire_branch_rules','questionnaire_publication_snapshots','participant_sessions',"
                    + "'participant_questionnaire_steps','participant_answers')"))
                    .containsExactly("9");
        }
    }

    private Flyway flyway(String schema, String target) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").schemas(schema).defaultSchema(schema);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private List<String> values(Connection connection, String sql) throws Exception {
        List<String> values = new ArrayList<>();
        try (var statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                values.add(result.getString(1));
            }
        }
        return values;
    }
}

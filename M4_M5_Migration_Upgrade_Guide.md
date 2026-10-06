# M4/M5 mainline migration and local data upgrade

## Supported histories

The canonical release preserves main V1-V10 exactly. V10 remains
`V10__sync_all_platform_feed_templates.sql`; M4/M5 is appended as:

| Version | Script |
| --- | --- |
| V11 | create_question_bank_tables |
| V12 | create_questionnaires |
| V13 | add_questionnaire_branch_rules |
| V14 | create_questionnaire_publication_snapshots |
| V15 | create_participant_sessions |
| V16 | create_participant_questionnaire_runtime |

New databases initialize normally. Standard main databases at V9 or V10
can apply remaining migrations after backup and upgrade validation.
Automated PostgreSQL tests verify both baselines, preserving applied
version/script/checksum/success records and existing account/study/feed data.
Check actual history first if an environment has custom or failed migrations.

## Legacy feature history: do not restart in place

Older feature databases used V10 question-bank, V11 questionnaire, V12 branch
rules, V13 snapshot, V14 sessions, V15 answers, and V16 template sync.
Those applied versions have different meanings. Renaming files does not
rewrite their database history. Do not disable validation, run `repair`,
modify history records, or remove volumes to bypass the mismatch.

Use a separate new database initialized by canonical code, then copy the
business data and accept it before changing the application's connection.
The source database and its Flyway history remain untouched.

## Copy utility (PowerShell, local Docker)

`scripts/copy-legacy-m4-m5-database.ps1` is deliberately limited to the known
legacy feature history and known business tables. It requires the source
backend to be stopped, refuses an existing destination, makes a full dump
and asset backup, initializes a new database using the candidate image,
copies only business data in one transaction, and compares counts and
whole-table digests for all 13 business tables. Schema columns must match.
The source history is checked unchanged. Secrets are read from existing
container configuration in memory and are not printed or written by the utility.

Example only; verify names, credentials, network, and image first:

```powershell
docker compose stop backend frontend
docker compose build backend frontend

.\scripts\copy-legacy-m4-m5-database.ps1 `
  -SourceDatabase survey_platform `
  -TargetDatabase survey_platform_mainline `
  -BackupDirectory 'C:\verified-local-path\new-unique-backup-directory'
```

If Docker is not on PATH, supply `-DockerExe` with its verified executable
path. The default container/network/image names match this repository's
Windows local Compose setup; pass explicit alternatives elsewhere. The
utility is not a generic production migration tool and is not for main V10
databases, which follow normal forward migrations.

The utility does not import old `flyway_schema_history` or overwrite the
canonical system `feed_templates`. Existing `study_feeds` and published
questionnaire snapshots are copied as independent business data. If system
templates were manually customized, review that difference before cutover.
Additional tables or unknown source histories stop the utility for review.
No database is deleted, including after a failed copy. Inspect failures,
retain the old application configuration, and restore service with the old
compatible image if needed. Failed candidates must be reviewed before any
separate cleanup; use a fresh destination for a later attempt.

## Acceptance and cutover

After updating a previously compiled checkout, run `./mvnw clean test`
(Windows: `.\mvnw.cmd clean test`) rather than reusing stale build resources.
Renamed migration files can otherwise remain duplicated under `target/classes`.
The Docker build starts from clean source and does not reuse that directory.

1. Keep a code recovery ref, verified full dump, assets, and private `.env` backup.
2. Run backend/performance and frontend regressions, fresh initialization,
   and main V9/V10 upgrade tests.
3. Restore the source backup in isolation and rehearse the same copy utility.
4. Verify IDs, timestamps, ownership, feeds, questions/options, branch rules,
   snapshots, sessions, visited paths, answers, and asset references.
5. Keep the backend timeout scheduler disabled during the candidate checks.
6. With writes still stopped, perform the verified copy against the actual
   PostgreSQL container, creating a new database, not overwriting the old one.
7. Back up the ignored local `.env`, change only `POSTGRES_DB` to the verified
   destination, then use the standard `down` / `up -d --build --force-recreate`
   redeployment without `--volumes`. Existing database and asset volumes stay.
8. Verify startup, canonical V1-V16 history, business digests, HTTP access,
   and existing study entry points. Do not write acceptance data to the real
   database; use an isolated copy for participant workflows.

The original database remains in the same PostgreSQL volume. To roll back,
first protect all writes made after cutover, then restore the old connection
configuration **and** compatible old image/code. Old code cannot safely
point at canonical history, and new code cannot point at legacy history.
Do not reset/rewrite the shared branch or change migration metadata as rollback.

## Demonstration versus production

The regular local Compose file explicitly uses `dev`. Server-style demos
must add `docker-compose.demo.yml` to the base server file; use the same
project name across updates. It enables only the explicit dev completion
gate and configures the public participant base URL. Real M6 completion and
approved consent are not supplied by this change. Production remains
fail-closed until the real completion adapter is implemented and tested.

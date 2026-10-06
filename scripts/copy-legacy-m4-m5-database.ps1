# Copies known legacy M4/M5 business data into a NEW mainline-initialized database.
# Does not modify source data/history, switch application configuration, or delete databases.
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$SourceDatabase,
    [Parameter(Mandatory)][string]$TargetDatabase,
    [Parameter(Mandatory)][string]$BackupDirectory,
    [string]$DbContainer = 'survey-platform-db',
    [string]$BackendContainer = 'survey-platform-backend',
    [string]$Network = 'cs-42-3_default',
    [string]$CandidateImage = 'cs-42-3-backend',
    [string]$DockerExe = 'docker'
)
$ErrorActionPreference = 'Stop'
foreach ($dbName in @($SourceDatabase, $TargetDatabase)) {
    if ($dbName -notmatch '^[a-z][a-z0-9_]{0,62}$') { throw 'Use simple PostgreSQL database names.' }
}
if ($SourceDatabase -eq $TargetDatabase) { throw 'Source and target must differ.' }
function Invoke-Docker([string[]]$Arguments) {
    $output = & $DockerExe @Arguments
    if ($LASTEXITCODE -ne 0) { throw ('Docker operation failed: ' + $Arguments[0]) }
    return $output
}
function Sql([string]$Database, [string]$Query) {
    Invoke-Docker -Arguments @('exec', $DbContainer, 'psql', '-X', '-v', 'ON_ERROR_STOP=1', '-U', $dbUser,
        '-d', $Database, '-At', '-c', $Query)
}
function Digests([string]$Database) {
    $result = @{}
    foreach ($table in $businessTables) {
        $query = "SELECT count(*) || '|' || md5(COALESCE(string_agg(row_to_json(t)::text, E'\n' ORDER BY row_to_json(t)::text), '')) FROM $table t;"
        $result[$table] = [string](Sql $Database $query)
    }
    return $result
}
$businessTables = @('researchers','studies','study_assets','study_feeds','questions','question_options',
    'questionnaires','questionnaire_items','questionnaire_branch_rules','questionnaire_publication_snapshots',
    'participant_sessions','participant_questionnaire_steps','participant_answers')
$dbDetails = (Invoke-Docker -Arguments @('inspect', $DbContainer) | ConvertFrom-Json)[0]
$backendDetails = (Invoke-Docker -Arguments @('inspect', $BackendContainer) | ConvertFrom-Json)[0]
if ($backendDetails.State.Running) { throw 'Stop the source backend first to prevent concurrent writes.' }
$dbEnv = @{}
foreach ($entry in $dbDetails.Config.Env) {
    $pair = $entry.Split('=', 2)
    if ($pair.Length -eq 2) { $dbEnv[$pair[0]] = $pair[1] }
}
$backendEnv = @{}
foreach ($entry in $backendDetails.Config.Env) {
    $pair = $entry.Split('=', 2)
    if ($pair.Length -eq 2) { $backendEnv[$pair[0]] = $pair[1] }
}
$dbUser = $dbEnv['POSTGRES_USER']
$dbPassword = $dbEnv['POSTGRES_PASSWORD']
if (!$dbUser -or !$dbPassword -or !$backendEnv['JWT_SECRET']) { throw 'Missing container database/JWT configuration.' }
$expectedLegacyScripts = @(
    'V10__create_question_bank_tables.sql','V11__create_questionnaires.sql',
    'V12__add_questionnaire_branch_rules.sql','V13__create_questionnaire_publication_snapshots.sql',
    'V14__create_participant_sessions.sql','V15__create_participant_questionnaire_runtime.sql',
    'V16__sync_all_platform_feed_templates.sql')
$actualLegacyScripts = @(Sql $SourceDatabase "SELECT script FROM flyway_schema_history WHERE version::int BETWEEN 10 AND 16 AND success ORDER BY installed_rank;")
if (($actualLegacyScripts -join '|') -ne ($expectedLegacyScripts -join '|')) {
    throw 'Source is not the supported legacy feature history. Do not use this script for standard main databases.'
}
if ((Sql $SourceDatabase "SELECT count(*) FROM flyway_schema_history WHERE NOT success;") -ne '0') {
    throw 'Source has failed migrations; stop and investigate.'
}
if ((Sql $SourceDatabase "SELECT count(*) FROM pg_database WHERE datname='$TargetDatabase';") -ne '0') {
    throw 'Target database already exists. Never overwrite a database.'
}
$actualTables = @(Sql $SourceDatabase "SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename NOT IN ('flyway_schema_history','feed_templates') ORDER BY tablename;")
if (($actualTables -join '|') -ne (($businessTables | Sort-Object) -join '|')) {
    throw 'Unexpected source tables; review the copy scope before proceeding.'
}
$backupPath = [System.IO.Path]::GetFullPath($BackupDirectory)
if (Test-Path -LiteralPath $backupPath) { throw 'Use a new backup directory; existing backup paths are never overwritten.' }
New-Item -ItemType Directory -Path $backupPath -ErrorAction Stop | Out-Null
$operationId = Get-Date -Format 'yyyyMMddHHmmss'
$fullDump = '/tmp/m4m5-' + $operationId + '-full.dump'
$dataDump = '/tmp/m4m5-' + $operationId + '-business.dump'
$null = Invoke-Docker -Arguments @('exec',$DbContainer,'pg_dump','-U',$dbUser,'-d',$SourceDatabase,'-Fc','-f',$fullDump)
$null = Invoke-Docker -Arguments @('exec',$DbContainer,'pg_restore','-l',$fullDump)
$null = Invoke-Docker -Arguments @('cp',($DbContainer+':'+$fullDump),(Join-Path $backupPath 'source-full.dump'))
$null = Invoke-Docker -Arguments @('cp',($BackendContainer+':/app/data/assets'),(Join-Path $backupPath 'assets'))
$sourceDigests = Digests $SourceDatabase
$sourceHistory = [string](Sql $SourceDatabase "SELECT md5(string_agg(row_to_json(t)::text, E'\n' ORDER BY row_to_json(t)::text)) FROM flyway_schema_history t;")
$null = Invoke-Docker -Arguments @('exec',$DbContainer,'pg_dump','-U',$dbUser,'-d',$SourceDatabase,'-Fc','--data-only',
    '--exclude-table=public.flyway_schema_history','--exclude-table=public.feed_templates','-f',$dataDump)
$null = Invoke-Docker -Arguments @('cp',($DbContainer+':'+$dataDump),(Join-Path $backupPath 'business-only.dump'))
$null = Invoke-Docker -Arguments @('exec',$DbContainer,'createdb','-U',$dbUser,$TargetDatabase)
$candidateName = 'm4m5-copy-' + $operationId
$candidateCreated = $false
try {
    $null = Invoke-Docker -Arguments @('run','-d','--name',$candidateName,'--network',$Network,
        '-e',('SPRING_DATASOURCE_URL=jdbc:postgresql://'+$DbContainer+':5432/'+$TargetDatabase),
        '-e',('SPRING_DATASOURCE_USERNAME='+$dbUser),'-e',('SPRING_DATASOURCE_PASSWORD='+$dbPassword),
        '-e',('JWT_SECRET='+$backendEnv['JWT_SECRET']),'-e','SPRING_PROFILES_ACTIVE=dev',
        '-e','PARTICIPANT_TIMEOUT_SCHEDULER_ENABLED=false',$CandidateImage)
    $candidateCreated = $true
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        $logs = Invoke-Docker -Arguments @('logs',$candidateName)
        if (($logs -join "`n") -match 'Started SurveyPlatformBackendApplication') { $ready=$true; break }
        $state = Invoke-Docker -Arguments @('inspect',$candidateName,'--format','{{.State.Running}}')
        if ($state -ne 'true') { throw 'Candidate backend failed to start; inspect its saved logs.' }
        Start-Sleep -Seconds 2
    }
    if (!$ready) { throw 'Candidate initialization timed out.' }
    $null = Invoke-Docker -Arguments @('stop',$candidateName)
    if ((Sql $TargetDatabase "SELECT count(*) FROM flyway_schema_history WHERE version IS NOT NULL AND success;") -ne '16') {
        throw 'Target initialization is incomplete.'
    }
    if ((Sql $TargetDatabase "SELECT script FROM flyway_schema_history WHERE version='10';") -ne 'V10__sync_all_platform_feed_templates.sql') {
        throw 'Candidate image does not use mainline migration history.'
    }
    $sourceColumns = @(Sql $SourceDatabase "SELECT table_name || '|' || column_name || '|' || data_type || '|' || udt_name FROM information_schema.columns WHERE table_schema='public' AND table_name NOT IN ('flyway_schema_history','feed_templates') ORDER BY table_name,ordinal_position;")
    $targetColumns = @(Sql $TargetDatabase "SELECT table_name || '|' || column_name || '|' || data_type || '|' || udt_name FROM information_schema.columns WHERE table_schema='public' AND table_name NOT IN ('flyway_schema_history','feed_templates') ORDER BY table_name,ordinal_position;")
    if (($sourceColumns -join "`n") -ne ($targetColumns -join "`n")) { throw 'Business schemas differ; do not import data.' }
    $null = Invoke-Docker -Arguments @('exec',$DbContainer,'pg_restore','--exit-on-error','--single-transaction',
        '-U',$dbUser,'-d',$TargetDatabase,$dataDump)
    $targetDigests = Digests $TargetDatabase
    foreach ($table in $businessTables) {
        if ($sourceDigests[$table] -ne $targetDigests[$table]) { throw ('Copy verification failed: '+$table) }
        Write-Output ($table+'|'+$targetDigests[$table]+'|MATCH')
    }
    if ($sourceHistory -ne (Sql $SourceDatabase "SELECT md5(string_agg(row_to_json(t)::text, E'\n' ORDER BY row_to_json(t)::text)) FROM flyway_schema_history t;")) {
        throw 'Source migration history changed unexpectedly.'
    }
    $sourceAfter = Digests $SourceDatabase
    foreach ($table in $businessTables) {
        if ($sourceAfter[$table] -ne $sourceDigests[$table]) { throw ('Source changed during copy: '+$table) }
    }
    Write-Output ('VERIFIED: all 13 business tables match; source history unchanged; target='+$TargetDatabase)
    Write-Output ('BACKUP: '+$backupPath)
    Write-Output 'Application configuration was NOT switched. Cut over only after separate acceptance checks.'
} finally {
    if ($candidateCreated) {
        Invoke-Docker -Arguments @('logs',$candidateName) | Out-File -LiteralPath (Join-Path $backupPath 'candidate-startup.log') -Encoding utf8
        $null = Invoke-Docker -Arguments @('stop',$candidateName)
        $null = Invoke-Docker -Arguments @('rm',$candidateName)
    }
}

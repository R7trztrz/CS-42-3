Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$projectName = "survey-verify-$([guid]::NewGuid().ToString('N').Substring(0, 8))"
$envFile = Join-Path ([System.IO.Path]::GetTempPath()) "$projectName.env"
$webPort = $null
$composeArgs = @(
    "compose",
    "--project-name", $projectName,
    "--env-file", $envFile,
    "--file", "docker-compose.server.yml",
    "--file", "docker-compose.demo.yml"
)

function Invoke-Compose {
    param([string[]]$Arguments)

    & docker @script:composeArgs @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose failed: $($Arguments -join ' ')"
    }
}

function Get-ComposeOutput {
    param([string[]]$Arguments)

    $output = & docker @script:composeArgs @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose failed: $($Arguments -join ' ')"
    }
    return ($output | Out-String).Trim()
}

function Wait-ForHealthyService {
    param(
        [string]$Service,
        [int]$TimeoutSeconds = 180
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $containerId = Get-ComposeOutput -Arguments @("ps", "--quiet", $Service)
        if ($containerId) {
            $health = & docker inspect --format '{{.State.Health.Status}}' $containerId
            if ($LASTEXITCODE -eq 0 -and $health -eq "healthy") {
                Write-Host "PASS: $Service is healthy"
                return
            }

            $state = & docker inspect --format '{{.State.Status}}' $containerId
            if ($LASTEXITCODE -eq 0 -and $state -eq "exited") {
                throw "$Service stopped before becoming healthy. Check Docker Desktop logs."
            }
        }
        Start-Sleep -Seconds 3
    }

    throw "$Service did not become healthy within $TimeoutSeconds seconds."
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker CLI was not found. Start Docker Desktop and run this script from PowerShell."
}

$serverVersion = & docker version --format '{{.Server.Version}}'
if ($LASTEXITCODE -ne 0 -or -not $serverVersion) {
    throw "Docker Desktop is not ready. Start it, wait for Engine running, then retry."
}

$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$listener.Start()
$webPort = $listener.LocalEndpoint.Port
$listener.Stop()
$participantBaseUrl = "http://localhost:$webPort"

$randomBytes = New-Object byte[] 32
$randomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $randomGenerator.GetBytes($randomBytes)
}
finally {
    $randomGenerator.Dispose()
}
$databasePassword = [System.BitConverter]::ToString($randomBytes).Replace("-", "").ToLowerInvariant()
$jwtSecret = [System.Convert]::ToBase64String($randomBytes)

$environment = @"
WEB_BIND_ADDRESS=127.0.0.1
WEB_PORT=$webPort
APP_PARTICIPANT_BASE_URL=$participantBaseUrl
POSTGRES_DB=survey_platform
POSTGRES_USER=survey_user
POSTGRES_PASSWORD=$databasePassword
JWT_SECRET=$jwtSecret
TURNSTILE_SECRET=1x0000000000000000000000000000000AA
VITE_TURNSTILE_SITE_KEY=1x00000000000000000000AA
"@

Set-Content -LiteralPath $envFile -Value $environment -Encoding Ascii -ErrorAction Stop

Push-Location -LiteralPath $repoRoot -ErrorAction Stop
try {
    Invoke-Compose -Arguments @("config", "--quiet")
    Write-Host "PASS: Docker Compose configuration is valid"

    Invoke-Compose -Arguments @("up", "--detach", "--build")
    Wait-ForHealthyService -Service "db"
    Wait-ForHealthyService -Service "frontend"

    $backendUrl = $null
    $deadline = (Get-Date).AddSeconds(180)
    while ((Get-Date) -lt $deadline) {
        $backendUrl = & docker @composeArgs exec -T backend printenv APP_PARTICIPANT_BASE_URL 2>$null
        if ($LASTEXITCODE -eq 0 -and ($backendUrl | Out-String).Trim() -eq $participantBaseUrl) {
            break
        }
        Start-Sleep -Seconds 3
    }
    if (($backendUrl | Out-String).Trim() -ne $participantBaseUrl) {
        throw "The backend did not receive the expected participant URL."
    }
    Write-Host "PASS: backend received APP_PARTICIPANT_BASE_URL"

    $healthResponse = Invoke-WebRequest -Uri "$participantBaseUrl/healthz" -UseBasicParsing -TimeoutSec 10
    if ($healthResponse.StatusCode -ne 200 -or $healthResponse.Content.Trim() -ne "ok") {
        throw "The frontend health endpoint did not return HTTP 200 and 'ok'."
    }
    Write-Host "PASS: frontend health endpoint returned HTTP 200 and ok"

    $homeResponse = Invoke-WebRequest -Uri $participantBaseUrl -UseBasicParsing -TimeoutSec 10
    if ($homeResponse.StatusCode -ne 200) {
        throw "The application page did not return HTTP 200."
    }
    Write-Host "PASS: application page returned HTTP 200"
    Write-Host "All smoke checks passed. This is a local demonstration, not a production deployment."
}
finally {
    try {
        Invoke-Compose -Arguments @("down", "--volumes", "--remove-orphans")
    }
    catch {
        Write-Warning "Could not automatically remove the temporary Docker demo. Run the cleanup command shown in the deployment guide."
    }
    Remove-Item -LiteralPath $envFile -Force -ErrorAction SilentlyContinue
    Pop-Location
}

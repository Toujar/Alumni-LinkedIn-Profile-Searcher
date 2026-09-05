# run.ps1 - Loads .env and starts the Spring Boot application
# Usage: .\run.ps1

$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Error ".env file not found. Copy .env.example to .env and fill in your values."
    exit 1
}

# Parse .env into a hashtable
$envVars = @{}
Get-Content $envFile |
    Where-Object { $_ -notmatch '^\s*#' -and $_ -match '=' } |
    ForEach-Object {
        $parts = $_ -split '=', 2
        $envVars[$parts[0].Trim()] = $parts[1].Trim()
    }

# Build -D arguments to pass directly to the JVM (bypasses shell env inheritance issues)
$jvmArgs = ($envVars.GetEnumerator() | ForEach-Object { "-D$($_.Key)=$($_.Value)" }) -join ' '

Write-Host "Starting Alumni LinkedIn Profile Searcher..."
Write-Host "Agent ID: $($envVars['PHANTOMBUSTER_AGENT_ID'])"

$mavenArgs = "spring-boot:run"
foreach ($kv in $envVars.GetEnumerator()) {
    $mavenArgs += " `"-D$($kv.Key)=$($kv.Value)`""
}

$cmd = "mvn $mavenArgs"
Invoke-Expression $cmd

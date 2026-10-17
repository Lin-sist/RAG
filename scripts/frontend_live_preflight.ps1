param(
    [switch]$Json,
    [ValidateRange(100, 2000)]
    [int]$ConnectTimeoutMs = 500
)

# Report only: loopback TCP probes and local file/tool checks. No HTTP, auth,
# Docker mutations, service startup, environment-file reads or provider calls.
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path

function Test-LoopbackPort {
    param([int]$Port)
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $pending = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        try {
            if (-not $pending.AsyncWaitHandle.WaitOne($ConnectTimeoutMs)) { return $false }
            $client.EndConnect($pending)
            return $client.Connected
        } finally { $pending.AsyncWaitHandle.Close() }
    } catch { return $false }
    finally { $client.Close() }
}

$services = @(
    @{ name = 'frontend'; port = 5173 },
    @{ name = 'backend'; port = 8080 },
    @{ name = 'mysql'; port = 3306 },
    @{ name = 'redis'; port = 6379 },
    @{ name = 'milvus'; port = 19530 }
) | ForEach-Object {
    [pscustomobject]@{ name = $_.name; host = '127.0.0.1'; port = $_.port; tcpReachable = (Test-LoopbackPort $_.port) }
}
$tools = @('git', 'java', 'mvn', 'node', 'docker') | ForEach-Object {
    [pscustomobject]@{ name = $_; available = [bool](Get-Command $_ -ErrorAction SilentlyContinue) }
}
$fixturePath = Join-Path $repoRoot 'rag-frontend/tests/fixtures/frontend-live-source.txt'
$fixture = $null
if (Test-Path -LiteralPath $fixturePath) {
    $bytes = [System.IO.File]::ReadAllBytes($fixturePath)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try { $digest = ([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-', '').ToLowerInvariant() }
    finally { $sha.Dispose() }
    $fixture = [pscustomobject]@{
        path = 'rag-frontend/tests/fixtures/frontend-live-source.txt'
        bytes = $bytes.Length
        sha256 = $digest
        belowOneKiB = $bytes.Length -lt 1024
    }
}
$missing = @($services | Where-Object { -not $_.tcpReachable } | ForEach-Object { $_.name })
$report = [pscustomobject][ordered]@{
    schemaVersion = 'frontend-live-preflight-v1'
    observedAtUtc = [DateTime]::UtcNow.ToString('o')
    status = $(if ($missing.Count -gt 0) { 'PREREQUISITES_MISSING' } else { 'TCP_REACHABLE_ONLY' })
    services = @($services)
    missing = $missing
    tools = @($tools)
    fixture = $fixture
    frontendDependenciesPresent = Test-Path -LiteralPath (Join-Path $repoRoot 'rag-frontend/node_modules/vue-tsc/bin/vue-tsc.js')
    realBusinessCalls = 0
    providerCalls = 0
    serviceStarts = 0
    businessWrites = 0
    limitations = @(
        'TCP reachability does not identify a service or prove health, auth, data or provider readiness.',
        'Ports are repository defaults; changed deployment ports need separate verification.',
        'Configuration defaults and credentials are not runtime-verified; no .env or protected configuration is read.',
        'A successful preflight never authorizes live execution or starts services.'
    )
}
if ($Json) { $report | ConvertTo-Json -Depth 6 }
else {
    Write-Output "Report status: $($report.status)"
    $report.services | Format-Table name, host, port, tcpReachable -AutoSize
    Write-Output 'Report only; real business/provider calls, writes and service starts: 0.'
    Write-Output 'TCP reachability is not a business or provider acceptance result.'
}
# Missing prerequisites are a reported fact, not a test failure or install trigger.
exit 0

param(
    [string]$BaseUrl = "http://localhost:8080",
    [long]$KbId = 2,
    [string]$Username = "admin",
    [string]$Password = "admin123",
    [int]$TopK = 6,
    [double]$MinScore = 0.3,
    [string]$QuestionsFile = "test-data/qa-phase0-questions.txt"
)

$ErrorActionPreference = "Stop"

function Read-Questions {
    param([string]$FilePath)

    if (Test-Path $FilePath) {
        return Get-Content -Path $FilePath -Encoding UTF8 |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
            ForEach-Object { [string]$_ }
    }

    return @(
        "什么是RAG",
        "为什么需要向量化",
        "知识库没有该内容时应如何回答"
    )
}

function Invoke-JsonPost {
    param(
        [string]$Uri,
        [string]$JsonBody,
        [hashtable]$Headers
    )

    $tempFile = [System.IO.Path]::GetTempFileName()
    try {
        Set-Content -Path $tempFile -Value $JsonBody -Encoding utf8

        $args = @('-sS', '-X', 'POST', $Uri, '-H', 'Content-Type: application/json; charset=utf-8')
        if ($Headers) {
            foreach ($key in $Headers.Keys) {
                $args += @('-H', ("{0}: {1}" -f $key, $Headers[$key]))
            }
        }
        $args += @('--data-binary', ("@{0}" -f $tempFile))

        $raw = & curl.exe @args
        return $raw | ConvertFrom-Json
    }
    finally {
        Remove-Item -Path $tempFile -ErrorAction SilentlyContinue
    }
}

function Login {
    param(
        [string]$BaseUrl,
        [string]$Username,
        [string]$Password
    )

    $payload = @{
        username = $Username
        password = $Password
    } | ConvertTo-Json

    $resp = Invoke-JsonPost -Uri "$BaseUrl/auth/login" -JsonBody $payload -Headers @{}

    if (-not $resp.data.accessToken) {
        throw "Login failed: accessToken is empty"
    }

    return $resp.data.accessToken
}

function Ensure-Dir {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

try {
    $questions = Read-Questions -FilePath $QuestionsFile
    if ($questions.Count -eq 0) {
        throw "No questions found"
    }

    $token = Login -BaseUrl $BaseUrl -Username $Username -Password $Password

    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $outDir = Join-Path "diagnostics/qa-snapshots" $timestamp
    Ensure-Dir -Path $outDir

    $summaryRows = @()

    for ($i = 0; $i -lt $questions.Count; $i++) {
        $question = [string]$questions[$i]

        $reqBody = @{
            kbId = $KbId
            question = $question
            topK = $TopK
            minScore = $MinScore
            enableCache = $false
        } | ConvertTo-Json

        $resp = Invoke-JsonPost -Uri "$BaseUrl/api/qa/ask" -JsonBody $reqBody -Headers @{ Authorization = "Bearer $token" }

        $rawPath = Join-Path $outDir ("q" + ($i + 1) + ".json")
        $resp | ConvertTo-Json -Depth 12 | Out-File -FilePath $rawPath -Encoding utf8

        $answer = $resp.data.answer
        $meta = $resp.data.metadata
        $citations = $resp.data.citations

        $summaryRows += [PSCustomObject]@{
            idx = $i + 1
            question = $question
            answerLength = if ($answer) { $answer.Length } else { 0 }
            contextCount = if ($meta.contextCount -ne $null) { $meta.contextCount } else { 0 }
            retrievedContextCount = if ($meta.retrievedContextCount -ne $null) { $meta.retrievedContextCount } else { 0 }
            removedByBudget = if ($meta.removedByBudget -ne $null) { $meta.removedByBudget } else { 0 }
            retrievedTopScore = if ($meta.retrievedTopScore -ne $null) { $meta.retrievedTopScore } else { 0 }
            retrievedAvgScore = if ($meta.retrievedAvgScore -ne $null) { $meta.retrievedAvgScore } else { 0 }
            citationCount = if ($citations) { $citations.Count } else { 0 }
        }
    }

    $summaryPath = Join-Path $outDir "summary.csv"
    $summaryRows | Export-Csv -NoTypeInformation -Encoding utf8 -Path $summaryPath

    Write-Host "Snapshot completed: $outDir"
    Write-Host "Summary: $summaryPath"
}
catch {
    Write-Error $_
    exit 1
}

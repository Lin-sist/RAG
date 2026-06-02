param(
    [string]$BaseUrl = "http://localhost:8080",
    [long]$KbId = 2,
    [string]$Username = "admin",
    [string]$Password = "admin123",
    [string]$QuestionsFile = "test-data/qa-phase0-questions.txt",
    [string]$TopKList = "4,6,8",
    [string]$MinScoreList = "0.20,0.30,0.40",
    [int]$DelayMs = 2200
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

function Parse-NumberList {
    param([string]$Raw)

    return $Raw.Split(',') |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -ne "" }
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

function To-IntValue {
    param([object]$Value)
    if ($null -eq $Value -or $Value -eq "") {
        return 0
    }
    return [int]$Value
}

function To-DoubleValue {
    param([object]$Value)
    if ($null -eq $Value -or $Value -eq "") {
        return 0.0
    }
    return [double]$Value
}

function Get-MetadataValue {
    param(
        [object]$Metadata,
        [string]$Key,
        [object]$DefaultValue
    )

    if ($null -eq $Metadata) {
        return $DefaultValue
    }

    if ($Metadata.PSObject.Properties.Name -contains $Key) {
        $val = $Metadata.$Key
        if ($null -ne $val -and $val -ne "") {
            return $val
        }
    }

    return $DefaultValue
}

try {
    $questions = Read-Questions -FilePath $QuestionsFile
    if ($questions.Count -eq 0) {
        throw "No questions found"
    }

    $topKs = Parse-NumberList -Raw $TopKList | ForEach-Object { [int]$_ }
    $minScores = Parse-NumberList -Raw $MinScoreList | ForEach-Object { [double]$_ }

    if ($topKs.Count -eq 0 -or $minScores.Count -eq 0) {
        throw "TopKList/MinScoreList cannot be empty"
    }

    $token = Login -BaseUrl $BaseUrl -Username $Username -Password $Password

    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss-fff"
    $outDir = Join-Path "diagnostics/qa-grid" $timestamp
    Ensure-Dir -Path $outDir

    $summaryRows = @()

    foreach ($minScore in $minScores) {
        foreach ($topK in $topKs) {
            $comboRows = @()

            foreach ($question in $questions) {
                $reqBody = @{
                    kbId = $KbId
                    question = $question
                    topK = $topK
                    minScore = $minScore
                    enableCache = $false
                } | ConvertTo-Json

                $resp = Invoke-JsonPost -Uri "$BaseUrl/api/qa/ask" -JsonBody $reqBody -Headers @{ Authorization = "Bearer $token" }

                $status = ""
                if ($resp.data.metadata -and $resp.data.metadata.status) {
                    $status = [string]$resp.data.metadata.status
                }

                $comboRows += [PSCustomObject]@{
                    question = $question
                    answerLength = if ($resp.data.answer) { [string]$resp.data.answer.Length } else { "0" }
                    contextCount = [string](Get-MetadataValue -Metadata $resp.data.metadata -Key "contextCount" -DefaultValue 0)
                    retrievedContextCount = [string](Get-MetadataValue -Metadata $resp.data.metadata -Key "retrievedContextCount" -DefaultValue 0)
                    removedByBudget = [string](Get-MetadataValue -Metadata $resp.data.metadata -Key "removedByBudget" -DefaultValue 0)
                    retrievedTopScore = [string](Get-MetadataValue -Metadata $resp.data.metadata -Key "retrievedTopScore" -DefaultValue 0)
                    retrievedAvgScore = [string](Get-MetadataValue -Metadata $resp.data.metadata -Key "retrievedAvgScore" -DefaultValue 0)
                    citationCount = if ($resp.data.citations) { [string]$resp.data.citations.Count } else { "0" }
                    status = $status
                }

                Start-Sleep -Milliseconds $DelayMs
            }

            $detailFile = Join-Path $outDir ("detail-ms" + $minScore.ToString("0.00") + "-k" + $topK + ".csv")
            $comboRows | Export-Csv -NoTypeInformation -Encoding utf8 -Path $detailFile

            $summaryRows += [PSCustomObject]@{
                minScore = $minScore.ToString("0.00")
                topK = $topK
                avgAnswerLength = [math]::Round((($comboRows | Measure-Object -Property answerLength -Average).Average), 2)
                avgContextCount = [math]::Round((($comboRows | Measure-Object -Property contextCount -Average).Average), 2)
                avgRetrievedTopScore = [math]::Round((($comboRows | Measure-Object -Property retrievedTopScore -Average).Average), 4)
                avgRetrievedAvgScore = [math]::Round((($comboRows | Measure-Object -Property retrievedAvgScore -Average).Average), 4)
                avgCitationCount = [math]::Round((($comboRows | Measure-Object -Property citationCount -Average).Average), 2)
                noResultCount = ($comboRows | Where-Object { [string]$_.status -eq "no_result" }).Count
                detailFile = [System.IO.Path]::GetFileName($detailFile)
            }
        }
    }

    $summaryFile = Join-Path $outDir "grid-summary.csv"
    $summaryRows | Export-Csv -NoTypeInformation -Encoding utf8 -Path $summaryFile

    Write-Host "Grid experiment completed: $outDir"
    Write-Host "Summary: $summaryFile"
}
catch {
    Write-Error $_
    exit 1
}

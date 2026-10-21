param(
    [ValidateSet('Preflight', 'SensitiveLogs', 'FrontendBuild', 'DocAndScope', 'All')]
    [string]$Mode = 'Preflight'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$FrontendRoot = Join-Path $RepoRoot 'rag-frontend'

function Resolve-Executable {
    param([string]$Name, [string]$EnvironmentOverride)

    if ($EnvironmentOverride) {
        $candidate = [Environment]::GetEnvironmentVariable($EnvironmentOverride)
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }
    return $null
}

function Write-ToolStatus {
    param([string]$Name, [string]$Path)
    if ($Path) {
        Write-Host "[preflight] AVAILABLE $Name -> $Path"
    } else {
        Write-Host "[preflight] MISSING   $Name"
    }
}

function Invoke-Preflight {
    $git = Resolve-Executable 'git' $null
    $java = Resolve-Executable 'java' $null
    $maven = Resolve-Executable 'mvn' $null
    $python = Resolve-Executable 'python' 'RAG_PYTHON_EXE'
    $node = Resolve-Executable 'node' 'RAG_NODE_EXE'
    $npm = Resolve-Executable 'npm.cmd' 'RAG_NPM_EXE'

    Write-ToolStatus 'git' $git
    Write-ToolStatus 'java' $java
    Write-ToolStatus 'mvn' $maven
    Write-ToolStatus 'python' $python
    Write-ToolStatus 'node' $node
    Write-ToolStatus 'npm' $npm

    $vueTsc = Join-Path $FrontendRoot 'node_modules/vue-tsc/bin/vue-tsc.js'
    $vite = Join-Path $FrontendRoot 'node_modules/vite/bin/vite.js'
    $directFrontendFallback = $node -and (Test-Path -LiteralPath $vueTsc) -and (Test-Path -LiteralPath $vite)
    Write-Host "[preflight] INFO      frontend node_modules -> $(Test-Path -LiteralPath (Join-Path $FrontendRoot 'node_modules'))"
    Write-Host "[preflight] INFO      direct vue-tsc/vite fallback -> $directFrontendFallback"
    Write-Host '[preflight] RESULT    report-only (never installs tools or dependencies)'
}

function Invoke-SensitiveLogGate {
    $python = Resolve-Executable 'python' 'RAG_PYTHON_EXE'
    if (-not $python) {
        throw 'Python is required. Put python on PATH or set RAG_PYTHON_EXE.'
    }
    & $python -B (Join-Path $PSScriptRoot 'check_sensitive_logs.py') --root $RepoRoot
    if ($LASTEXITCODE -ne 0) {
        throw "Sensitive log gate failed with exit code $LASTEXITCODE."
    }
}

function Invoke-FrontendBuildGate {
    $npm = Resolve-Executable 'npm.cmd' 'RAG_NPM_EXE'
    Push-Location $FrontendRoot
    try {
        if ($npm) {
            & $npm run build
            if ($LASTEXITCODE -ne 0) {
                throw "npm run build failed with exit code $LASTEXITCODE."
            }
            return
        }

        $node = Resolve-Executable 'node' 'RAG_NODE_EXE'
        $vueTsc = Join-Path $FrontendRoot 'node_modules/vue-tsc/bin/vue-tsc.js'
        $vite = Join-Path $FrontendRoot 'node_modules/vite/bin/vite.js'
        if (-not $node -or -not (Test-Path -LiteralPath $vueTsc) -or -not (Test-Path -LiteralPath $vite)) {
            throw 'Frontend build requires npm, or node plus existing vue-tsc/vite node_modules. Set RAG_NODE_EXE when node is not on PATH.'
        }

        & $node $vueTsc -b
        if ($LASTEXITCODE -ne 0) {
            throw "vue-tsc -b failed with exit code $LASTEXITCODE."
        }
        & $node $vite build
        if ($LASTEXITCODE -ne 0) {
            throw "vite build failed with exit code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
}

function Invoke-DocAndScopeGate {
    $git = Resolve-Executable 'git' $null
    if (-not $git) {
        throw 'Git is required for DocAndScope gate.'
    }

    Push-Location $RepoRoot
    try {
        & $git diff --check
        if ($LASTEXITCODE -ne 0) {
            throw "git diff --check failed with exit code $LASTEXITCODE."
        }
        & $git diff --cached --check
        if ($LASTEXITCODE -ne 0) {
            throw "git diff --cached --check failed with exit code $LASTEXITCODE."
        }

        $statusLines = @(& $git status --porcelain)
        $protectedPrefixes = @('.env.local', 'rag-admin/src/main/resources/application-dev.yml', 'application-dev.yml', '.agents/', 'docs/学习文档/')
        $touchedPaths = @()
        foreach ($line in $statusLines) {
            if ($line.Length -gt 3) {
                $rawPath = $line.Substring(3).Trim()
                if ($rawPath -match ' -> ') {
                    $rawPath = ($rawPath -split ' -> ')[-1].Trim()
                }
                $rawPath = $rawPath.Trim('"') -replace '\\', '/'
                $touchedPaths += $rawPath
                foreach ($prefix in $protectedPrefixes) {
                    if ($rawPath -eq $prefix -or $rawPath.StartsWith($prefix)) {
                        throw "Protected path touched without explicit override: $rawPath"
                    }
                }
            }
        }

        $mdSet = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
        foreach ($coreDoc in @('AGENTS.md', '.ai/ACTIVE_TASK.md', '.ai/AGENT_LOG.md', '.ai/GOTCHAS.md', 'docs/workflow/vibecoding-playbook.md', 'openspec/project.md')) {
            $fullCore = Join-Path $RepoRoot $coreDoc
            if (Test-Path -LiteralPath $fullCore) {
                [void]$mdSet.Add((Resolve-Path -LiteralPath $fullCore).Path)
            }
        }
        foreach ($rel in $touchedPaths) {
            if ($rel.EndsWith('.md')) {
                $fullMd = Join-Path $RepoRoot $rel
                if (Test-Path -LiteralPath $fullMd) {
                    [void]$mdSet.Add((Resolve-Path -LiteralPath $fullMd).Path)
                }
            }
        }

        $linkRegex = [regex]'\[[^\]]*\]\(([^)]+)\)'
        $linksVerified = 0
        $brokenLinks = @()

        foreach ($mdFile in $mdSet) {
            $dir = Split-Path -Parent $mdFile
            $inFence = $false
            foreach ($line in (Get-Content -LiteralPath $mdFile -Encoding UTF8)) {
                if ($line -match '^\s*```') {
                    $inFence = -not $inFence
                    continue
                }
                if ($inFence) {
                    continue
                }
                $cleanLine = $line -replace '`[^`]*`', ''
                foreach ($m in $linkRegex.Matches($cleanLine)) {
                    $target = $m.Groups[1].Value.Trim()
                    if (-not $target -or $target.StartsWith('#') -or $target -match '^[a-zA-Z][a-zA-Z0-9+.-]*:' -or $target -match '[<>]') {
                        continue
                    }
                    $pathPart = ($target -split '#', 2)[0]
                    if (-not $pathPart) {
                        continue
                    }
                    $resolvedTarget = Join-Path $dir $pathPart
                    if (-not (Test-Path -LiteralPath $resolvedTarget)) {
                        $relMd = $mdFile.Substring($RepoRoot.Length).TrimStart('\', '/')
                        $brokenLinks += "${relMd} -> ${target}"
                    } else {
                        $linksVerified++
                    }
                }
            }
        }

        if ($brokenLinks.Count -gt 0) {
            throw ("Broken relative markdown links detected:`n" + ($brokenLinks -join "`n"))
        }

        $agentLogPath = Join-Path $RepoRoot '.ai/AGENT_LOG.md'
        $agentLogLines = 0
        if (Test-Path -LiteralPath $agentLogPath) {
            $agentLogLines = @(Get-Content -LiteralPath $agentLogPath -Encoding UTF8).Count
        }
        $logStatus = if ($agentLogLines -le 200) { "$agentLogLines (<=200 OK)" } else { "$agentLogLines (>200 SHARD_RECOMMENDED)" }
        Write-Host "[doc-scope] PASS (diff-check=OK, protected-paths=OK, md-files=$($mdSet.Count), links-verified=$linksVerified, agent-log-lines=$logStatus)"
    } finally {
        Pop-Location
    }
}

switch ($Mode) {
    'Preflight' {
        Invoke-Preflight
    }
    'SensitiveLogs' {
        Invoke-SensitiveLogGate
    }
    'FrontendBuild' {
        Invoke-FrontendBuildGate
    }
    'DocAndScope' {
        Invoke-DocAndScopeGate
    }
    'All' {
        Invoke-Preflight
        Invoke-DocAndScopeGate
        Invoke-SensitiveLogGate
        Invoke-FrontendBuildGate
    }
}

param(
    [ValidateSet('Preflight', 'SensitiveLogs', 'FrontendBuild', 'All')]
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
    'All' {
        Invoke-Preflight
        Invoke-SensitiveLogGate
        Invoke-FrontendBuildGate
    }
}

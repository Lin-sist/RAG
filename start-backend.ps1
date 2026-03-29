param(
	[switch]$WithDocker
)

$ErrorActionPreference = 'Stop'

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $rootDir

function Import-DotEnv {
	param([string]$Path)

	if (-not (Test-Path $Path)) {
		return
	}

	Get-Content $Path | ForEach-Object {
		$line = $_.Trim()
		if (-not $line -or $line.StartsWith('#')) {
			return
		}

		$idx = $line.IndexOf('=')
		if ($idx -lt 1) {
			return
		}

		$key = $line.Substring(0, $idx).Trim()
		$value = $line.Substring($idx + 1).Trim()

		if ((($value.StartsWith('"')) -and ($value.EndsWith('"'))) -or (($value.StartsWith("'")) -and ($value.EndsWith("'")))) {
			$value = $value.Substring(1, $value.Length - 2)
		}

		Set-Item -Path ("Env:{0}" -f $key) -Value $value
	}
}

Import-DotEnv -Path '.env.local'

if (-not $env:MYSQL_HOST_PORT) { $env:MYSQL_HOST_PORT = '3306' }
if (-not $env:REDIS_HOST_PORT) { $env:REDIS_HOST_PORT = '6379' }

if (-not $env:DB_URL) {
	$env:DB_URL = "jdbc:mysql://localhost:$($env:MYSQL_HOST_PORT)/rag_qa?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
}
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = 'root' }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = '123456' }
if (-not $env:REDIS_HOST) { $env:REDIS_HOST = 'localhost' }
if (-not $env:REDIS_PORT) { $env:REDIS_PORT = $env:REDIS_HOST_PORT }
if (-not $env:REDIS_PASSWORD) { $env:REDIS_PASSWORD = '123456' }

if ($WithDocker) {
	if (Test-Path '.env.local') {
		docker compose --env-file .env.local up -d
	} else {
		docker compose up -d
	}
}

try {
	$listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
		Select-Object -First 1
	if ($listener) {
		$ownerPid = $listener.OwningProcess
		$proc = Get-Process -Id $ownerPid -ErrorAction Stop
		if ($proc.ProcessName -in @('java', 'javaw')) {
			Write-Host "[start-backend] Port 8080 is used by $($proc.ProcessName) (PID=$ownerPid). Stopping it..."
			Stop-Process -Id $ownerPid -Force
			Start-Sleep -Seconds 1
		} else {
			throw "Port 8080 is in use by process '$($proc.ProcessName)' (PID=$ownerPid). Please stop it first."
		}
	}
} catch {
	throw "[start-backend] Port check failed: $($_.Exception.Message)"
}

Write-Host '[start-backend] Starting rag-admin with local env mapping on port 8080...'
mvn -f rag-admin/pom.xml spring-boot:run -DskipTests

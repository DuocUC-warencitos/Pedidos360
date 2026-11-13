# Funciones comunes de los scripts de despliegue Pedidos360.
# Se cargan con: . "$PSScriptRoot\_comun.ps1"

function Read-EnvFile {
    param([Parameter(Mandatory)][string]$Path)

    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($null -eq $line) { continue }
        $trimmed = $line.Trim()
        if ($trimmed -eq "" -or $trimmed.StartsWith("#")) { continue }
        $idx = $trimmed.IndexOf("=")
        if ($idx -lt 1) { continue }
        $key = $trimmed.Substring(0, $idx).Trim()
        $value = $trimmed.Substring($idx + 1).Trim()
        if ($value.StartsWith("#")) {
            $value = ""
        }
        else {
            $commentIdx = $value.IndexOf(" #")
            if ($commentIdx -ge 0) { $value = $value.Substring(0, $commentIdx).Trim() }
        }
        if ($value.Length -ge 2) {
            $first = $value[0]
            $last = $value[$value.Length - 1]
            if (($first -eq '"' -and $last -eq '"') -or ($first -eq "'" -and $last -eq "'")) {
                $value = $value.Substring(1, $value.Length - 2)
            }
        }
        $values[$key] = $value
    }
    return $values
}

function Write-EnvFile {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][AllowEmptyString()][AllowEmptyCollection()][string[]]$Lines
    )

    Set-Content -LiteralPath $Path -Value $Lines -Encoding ascii
}

function Merge-EnvLines {
    param(
        [Parameter(Mandatory)][string[]]$BaseLines,
        [hashtable]$Overrides
    )

    if (-not $Overrides -or $Overrides.Count -eq 0) { return $BaseLines }

    $result = New-Object System.Collections.Generic.List[string]
    $used = @{}

    foreach ($line in $BaseLines) {
        $trimmed = $line.Trim()
        if ($trimmed -eq "" -or $trimmed.StartsWith("#")) { $result.Add($line); continue }
        $idx = $trimmed.IndexOf("=")
        if ($idx -lt 1) { $result.Add($line); continue }

        $key = $trimmed.Substring(0, $idx).Trim()
        if ($Overrides.ContainsKey($key)) {
            $result.Add("$key=$($Overrides[$key])")
            $used[$key] = $true
        }
        else { $result.Add($line) }
    }

    foreach ($key in $Overrides.Keys) {
        if (-not $used.ContainsKey($key)) { $result.Add("$key=$($Overrides[$key])") }
    }

    return $result.ToArray()
}

function Get-EnvsDir {
    param([Parameter(Mandatory)][string]$ScriptRoot)
    return (Join-Path $ScriptRoot "..\envs")
}

function New-GeneratedSecret {
    param([int]$Length = 24)

    $chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $bytes = New-Object byte[] $Length
    $rng.GetBytes($bytes)

    $sb = New-Object System.Text.StringBuilder
    foreach ($b in $bytes) { [void]$sb.Append($chars[$b % $chars.Length]) }
    return $sb.ToString()
}

function New-KafkaClusterId {
    # Kafka espera un UUID base64 URL-safe de 22 caracteres (mismo formato que
    # `kafka-storage.sh random-uuid`): 16 bytes -> base64 -> 22 sin padding.
    $bytes = New-Object byte[] 16
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    return ([Convert]::ToBase64String($bytes)).Replace('+', '-').Replace('/', '_').TrimEnd('=')
}

function Save-EnvValues {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][hashtable]$Values
    )

    $lines = New-Object System.Collections.Generic.List[string]
    $used = @{}

    if (Test-Path -LiteralPath $Path) {
        foreach ($line in Get-Content -LiteralPath $Path) {
            if ($null -eq $line) { continue }
            $trimmed = $line.Trim()
            if ($trimmed -ne "" -and -not $trimmed.StartsWith("#")) {
                $idx = $trimmed.IndexOf("=")
                if ($idx -ge 1) {
                    $key = $trimmed.Substring(0, $idx).Trim()
                    if ($Values.ContainsKey($key)) {
                        $lines.Add("$key=$($Values[$key])")
                        $used[$key] = $true
                        continue
                    }
                }
            }
            $lines.Add($line)
        }
    }

    foreach ($key in $Values.Keys) {
        if (-not $used.ContainsKey($key)) { $lines.Add("$key=$($Values[$key])") }
    }

    Write-EnvFile -Path $Path -Lines $lines.ToArray()
}

function Initialize-ComunEnv {
    param(
        [Parameter(Mandatory)][string]$ScriptRoot,
        [string]$EnvDeployFile
    )

    $envsDir = Get-EnvsDir -ScriptRoot $ScriptRoot
    $comunPath = if ($EnvDeployFile) { $EnvDeployFile } else { Join-Path $envsDir "comun.env" }

    if ($EnvDeployFile -and -not (Test-Path -LiteralPath $comunPath)) {
        throw "No existe $comunPath."
    }

    if (-not (Test-Path -LiteralPath $comunPath)) {
        $legacy = Join-Path $ScriptRoot ".env.deploy"
        if (Test-Path -LiteralPath $legacy) {
            Write-Host "==> Usando $legacy (legado). Considera migrar a infra\envs\comun.env"
            return @{ Path = $legacy; Values = (Read-EnvFile -Path $legacy) }
        }

        $example = Join-Path $envsDir "comun.env.example"
        if (-not (Test-Path -LiteralPath $example)) { throw "No existe $example." }

        New-Item -ItemType Directory -Path $envsDir -Force | Out-Null
        Copy-Item -LiteralPath $example -Destination $comunPath
        Write-Host "==> Creado $comunPath a partir del ejemplo."
    }

    $values = Read-EnvFile -Path $comunPath
    $generated = @()

    if (-not $values["RABBITMQ_USERNAME"]) {
        $values["RABBITMQ_USERNAME"] = "pedidos360"
        $generated += "RABBITMQ_USERNAME"
    }
    if (-not $values["RABBITMQ_PASSWORD"]) {
        $values["RABBITMQ_PASSWORD"] = (New-GeneratedSecret -Length 24)
        $generated += "RABBITMQ_PASSWORD"
    }
    if (-not $values["RABBITMQ_ERLANG_COOKIE"]) {
        $values["RABBITMQ_ERLANG_COOKIE"] = (New-GeneratedSecret -Length 32)
        $generated += "RABBITMQ_ERLANG_COOKIE"
    }
    if (-not $values["INTERNAL_API_KEY"]) {
        $values["INTERNAL_API_KEY"] = (New-GeneratedSecret -Length 32)
        $generated += "INTERNAL_API_KEY"
    }
    if (-not $values["KAFKA_CLUSTER_ID"]) {
        $values["KAFKA_CLUSTER_ID"] = (New-KafkaClusterId)
        $generated += "KAFKA_CLUSTER_ID"
    }

    if ($generated.Count -gt 0) {
        Save-EnvValues -Path $comunPath -Values $values
        Write-Host "==> Generados automaticamente en ${comunPath}: $($generated -join ', ')"
    }

    return @{ Path = $comunPath; Values = $values }
}

function Get-ServiceOverrides {
    param(
        [Parameter(Mandatory)][string]$ScriptRoot,
        [Parameter(Mandatory)][string]$Service
    )

    $file = Join-Path (Get-EnvsDir -ScriptRoot $ScriptRoot) "$Service\.env"
    if (-not (Test-Path -LiteralPath $file)) { return @{} }

    $all = Read-EnvFile -Path $file
    $filtered = @{}
    foreach ($key in $all.Keys) {
        if ($all[$key] -ne "") { $filtered[$key] = $all[$key] }
    }

    if ($filtered.Count -eq 0) {
        Write-Host "==> infra\envs\$Service\.env existe pero no tiene overrides con valor (ignorado)"
        return @{}
    }

    Write-Host "==> Overrides aplicados de infra\envs\$Service\.env: $($filtered.Keys -join ', ')"
    return $filtered
}

function Get-TerraformExe {
    param([string]$Override)

    if ($Override) { return $Override }
    if ($env:TERRAFORM_EXE -and (Test-Path -LiteralPath $env:TERRAFORM_EXE)) { return $env:TERRAFORM_EXE }

    $cmd = Get-Command terraform.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $candidates = @(
        (Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Links\terraform.exe"),
        "C:\ProgramData\chocolatey\bin\terraform.exe",
        (Join-Path $env:USERPROFILE "scoop\shims\terraform.exe"),
        "C:\terraform\terraform.exe",
        "C:\tools\terraform\terraform.exe",
        (Join-Path $env:ProgramFiles "Terraform\terraform.exe")
    )

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }

    $wingetPackages = Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Packages"
    if (Test-Path -LiteralPath $wingetPackages) {
        foreach ($dir in Get-ChildItem -LiteralPath $wingetPackages -Directory -Filter "Hashicorp.Terraform*" -ErrorAction SilentlyContinue) {
            $exe = Get-ChildItem -LiteralPath $dir.FullName -Recurse -Filter "terraform.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($exe) { return $exe.FullName }
        }
    }

    throw "No encontre terraform.exe. Agrega terraform al PATH, setea TERRAFORM_EXE o pasa -TerraformExe <ruta>."
}

function Initialize-AwsCredentials {
    param(
        [Parameter(Mandatory)][string]$ScriptRoot,
        [switch]$SkipPrompt,
        [string]$AwsEnvFile
    )

    $envsDir = Get-EnvsDir -ScriptRoot $ScriptRoot
    if (-not $AwsEnvFile) { $AwsEnvFile = Join-Path $envsDir "aws.env" }

    if (Test-Path -LiteralPath $AwsEnvFile) {
        $all = Read-EnvFile -Path $AwsEnvFile
        $loaded = $false
        foreach ($key in @("AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY", "AWS_SESSION_TOKEN", "AWS_REGION")) {
            if ($all[$key]) {
                Set-Item -Path "env:$key" -Value $all[$key]
                $loaded = $true
            }
        }
        if ($loaded) {
            if (-not $all["AWS_SESSION_TOKEN"]) { Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue }
            Write-Host "==> Credenciales AWS cargadas desde $AwsEnvFile (solo en memoria del proceso)."
        }
    }

    if (-not $env:AWS_REGION) { $env:AWS_REGION = "us-east-1" }
    if (-not $env:AWS_DEFAULT_REGION) { $env:AWS_DEFAULT_REGION = $env:AWS_REGION }

    $hasCreds = ($env:AWS_ACCESS_KEY_ID -and $env:AWS_SECRET_ACCESS_KEY)

    if ($SkipPrompt) {
        if (-not $hasCreds) { Write-Host "==> Sin credenciales AWS en entorno ni en $AwsEnvFile (terraform usara ~/.aws o fallara)." }
        return
    }

    if ($hasCreds) {
        try {
            Test-AwsCredentials
            return
        }
        catch {
            Write-Host "==> Aviso: $($_.Exception.Message)"
        }
    }
    else {
        Write-Host "==> No hay credenciales AWS configuradas."
    }

    Write-Host "    Pega las del Learner Lab (Cloud Access); quedan solo en memoria y se pueden guardar en $AwsEnvFile."
    $accessKey = Read-Host "  AWS_ACCESS_KEY_ID (Enter para cancelar)"
    if (-not $accessKey) { throw "Deploy cancelado: se requieren credenciales AWS validas para terraform." }

    $secretKey = Read-Host "  AWS_SECRET_ACCESS_KEY"
    $sessionToken = Read-Host "  AWS_SESSION_TOKEN (Enter si no aplica)"
    $region = Read-Host "  AWS_REGION (Enter = us-east-1)"
    if (-not $region) { $region = "us-east-1" }

    $env:AWS_ACCESS_KEY_ID = $accessKey.Trim()
    $env:AWS_SECRET_ACCESS_KEY = $secretKey.Trim()
    if ($sessionToken) {
        $env:AWS_SESSION_TOKEN = $sessionToken.Trim()
    }
    else {
        Remove-Item Env:AWS_SESSION_TOKEN -ErrorAction SilentlyContinue
    }
    $env:AWS_REGION = $region.Trim()
    $env:AWS_DEFAULT_REGION = $env:AWS_REGION

    Test-AwsCredentials

    $save = Read-Host "  Guardar en $AwsEnvFile (sin versionar) para proximos deploys? [S/n]"
    if ($save -eq "" -or $save -match "^(s|y)") {
        if (-not (Test-Path -LiteralPath $AwsEnvFile)) {
            $example = Join-Path $envsDir "aws.env.example"
            if (Test-Path -LiteralPath $example) { Copy-Item -LiteralPath $example -Destination $AwsEnvFile }
        }
        Save-EnvValues -Path $AwsEnvFile -Values @{
            AWS_ACCESS_KEY_ID     = $env:AWS_ACCESS_KEY_ID
            AWS_SECRET_ACCESS_KEY = $env:AWS_SECRET_ACCESS_KEY
            AWS_SESSION_TOKEN     = $env:AWS_SESSION_TOKEN
            AWS_REGION            = $env:AWS_REGION
        }
        Write-Host "==> Guardado en $AwsEnvFile."
    }
}

function Test-AwsCredentials {
    $aws = Get-Command aws.exe -ErrorAction SilentlyContinue
    if (-not $aws) {
        Write-Host "==> AWS CLI no encontrado; omito validacion (terraform fallara si las credenciales no sirven)."
        return
    }

    $arn = & $aws.Source "sts" "get-caller-identity" "--query" "Arn" "--output" "text" 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $arn) {
        throw "Credenciales AWS invalidas o expiradas (aws sts get-caller-identity fallo). Renueva desde Cloud Access."
    }
    Write-Host "==> AWS OK: $arn"
}

function Invoke-Terraform {
    param(
        [Parameter(Mandatory)][string]$Exe,
        [Parameter(Mandatory)][string]$TerraformDir,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    & $Exe "-chdir=$TerraformDir" @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "terraform $($Arguments -join ' ') fallo con codigo $LASTEXITCODE."
    }
}

function Get-TerraformOutput {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$Exe,
        [Parameter(Mandatory)][string]$TerraformDir
    )

    $value = & $Exe "-chdir=$TerraformDir" "output" "-raw" $Name 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $value) {
        throw "No se pudo obtener el output '$Name' de terraform (corre 'terraform apply' primero)."
    }
    return (($value -join "`n").Trim())
}

function Get-TerraformOutputJson {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$Exe,
        [Parameter(Mandatory)][string]$TerraformDir
    )

    $raw = & $Exe "-chdir=$TerraformDir" "output" "-json" $Name 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $raw) {
        throw "No se pudo obtener el output '$Name' de terraform (corre 'terraform apply' primero)."
    }
    return (($raw -join "`n") | ConvertFrom-Json)
}

function Assert-SshKey {
    param(
        [string]$KeyPath,
        [Parameter(Mandatory)][hashtable]$Deploy,
        [Parameter(Mandatory)][string]$EnvPath
    )

    if (-not $KeyPath) { $KeyPath = $Deploy["AWS_KEY_PATH"] }
    if (-not $KeyPath -or -not (Test-Path -LiteralPath $KeyPath)) {
        throw "No existe la key SSH: '$KeyPath' (revisa AWS_KEY_PATH en $EnvPath)."
    }
    return $KeyPath
}

function Connect-RemoteGhcr {
    param(
        [Parameter(Mandatory)][hashtable]$Deploy,
        [Parameter(Mandatory)][string]$SshTarget,
        [Parameter(Mandatory)][AllowEmptyCollection()][string[]]$SshOptions
    )

    $username = $Deploy["GHCR_USERNAME"]
    $token = $Deploy["GHCR_TOKEN"]

    if (-not $username -or -not $token) {
        Write-Host "==> GHCR_USERNAME/GHCR_TOKEN vacios: se asume imagen GHCR publica (sin login en la EC2)."
        return
    }

    Write-Host "==> docker login ghcr.io en $SshTarget (usuario $username)"
    # 2>&1 remoto: el warning de docker login viaja por stdout y no rompe $ErrorActionPreference=Stop
    $token | ssh @SshOptions $SshTarget "docker login ghcr.io -u $username --password-stdin 2>&1"
    if ($LASTEXITCODE -ne 0) {
        throw "docker login ghcr.io fallo en $SshTarget. Revisa GHCR_USERNAME/GHCR_TOKEN (PAT con scope read:packages) o haz el paquete publico."
    }
    Write-Host "==> GHCR login OK en $SshTarget"
}

function Ensure-RemoteDisk {
    param(
        [Parameter(Mandatory)][string]$SshTarget,
        [Parameter(Mandatory)][AllowEmptyCollection()][string[]]$SshOptions
    )

    # Volumen EBS puede crecer en terraform: expande particion/filesystem si hace falta.
    $cmd = "if command -v growpart >/dev/null 2>&1; then sudo growpart /dev/nvme0n1 1 >/dev/null 2>&1 || true; sudo xfs_growfs / >/dev/null 2>&1 || true; fi; df -h / | tail -1"
    $out = (ssh @SshOptions $SshTarget $cmd 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) {
        Write-Host "==> Aviso: no se pudo verificar/expandir disco en ${SshTarget}: $out"
        return
    }
    Write-Host "==> Disco en ${SshTarget}: $out"
}

function Wait-RemoteSsh {
    param(
        [Parameter(Mandatory)][string]$SshTarget,
        [Parameter(Mandatory)][AllowEmptyCollection()][string[]]$SshOptions,
        [int]$TimeoutSeconds = 300
    )

    # terraform apply puede volver antes de que sshd este listo en una EC2 nueva.
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $attempt = 0
    while ((Get-Date) -lt $deadline) {
        $attempt++
        ssh @SshOptions -o ConnectTimeout=10 -o BatchMode=yes $SshTarget "true" 2>$null
        if ($LASTEXITCODE -eq 0) {
            if ($attempt -gt 1) { Write-Host "==> SSH listo en $SshTarget (intento $attempt)" }
            return
        }
        if ($attempt -eq 1) { Write-Host "==> Esperando SSH en $SshTarget..." }
        Start-Sleep -Seconds 5
    }
    throw "SSH no quedo disponible en $SshTarget tras $TimeoutSeconds segundos."
}

function Invoke-RemoteScript {
    param(
        [Parameter(Mandatory)][string]$SshTarget,
        [Parameter(Mandatory)][AllowEmptyCollection()][string[]]$SshOptions,
        [Parameter(Mandatory)][AllowEmptyString()][string]$Script
    )

    # No pipeamos el script a ssh: PowerShell agrega un newline de Windows (CRLF)
    # al final y bash recibe una linea extra con \r. Lo enviamos en base64 y lo
    # decodificamos en la EC2, asi no viaja ningun salto de linea problematico.
    $text = ($Script -replace "`r", "").TrimEnd()
    $b64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($text))
    ssh @SshOptions $SshTarget "echo '$b64' | base64 -d | bash -s" 2>&1 | ForEach-Object { Write-Host $_ }
}

function Wait-RemoteDocker {
    param(
        [Parameter(Mandatory)][string]$SshTarget,
        [Parameter(Mandatory)][AllowEmptyCollection()][string[]]$SshOptions,
        [int]$TimeoutSeconds = 600
    )

    # Si la EC2 es nueva (o cloud-init fallo), instala/arranca Docker y el plugin
    # compose. Es idempotente: en una EC2 ya configurada no hace nada.
    $bootstrap = @'
set -e
if ! command -v docker >/dev/null 2>&1; then
  echo "==> Docker no instalado; instalando..."
  sudo yum install -y docker git >/dev/null
fi
if ! sudo systemctl is-active --quiet docker; then
  echo "==> Iniciando Docker..."
  sudo systemctl enable --now docker >/dev/null 2>&1 || true
fi
sudo usermod -aG docker ec2-user >/dev/null 2>&1 || true
if ! docker compose version >/dev/null 2>&1; then
  echo "==> Instalando docker compose plugin..."
  sudo mkdir -p /usr/local/lib/docker/cli-plugins
  sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose
  sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
fi
'@
    [void](Invoke-RemoteScript -SshTarget $SshTarget -SshOptions $SshOptions -Script $bootstrap)

    # Cada intento abre una sesion SSH nueva: si usermod acaba de agregar el grupo
    # docker, la sesion siguiente ya lo tiene.
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $attempt = 0
    while ((Get-Date) -lt $deadline) {
        $attempt++
        ssh @SshOptions -o ConnectTimeout=10 $SshTarget "docker info >/dev/null 2>&1 && docker compose version >/dev/null 2>&1" 2>$null
        if ($LASTEXITCODE -eq 0) {
            if ($attempt -gt 1) { Write-Host "==> Docker listo en $SshTarget (intento $attempt)" }
            return
        }
        if ($attempt -eq 1) { Write-Host "==> Esperando Docker en $SshTarget..." }
        Start-Sleep -Seconds 10
    }
    throw "Docker no quedo listo en $SshTarget tras $TimeoutSeconds segundos."
}

function Get-HttpStatus {
    param(
        [Parameter(Mandatory)][string]$Url,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 15
    )

    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    if (-not $curl) { throw "curl.exe no encontrado; no se puede verificar $Url." }

    # --ssl-no-revoke: en Windows schannel falla si el servidor de revocacion no responde.
    $curlArgs = @("-sS", "--ssl-no-revoke", "-o", "NUL", "-w", "%{http_code}", "--max-time", "$TimeoutSeconds", "-X", $Method, $Url)
    foreach ($key in $Headers.Keys) { $curlArgs += @("-H", "${key}: $($Headers[$key])") }

    $out = & $curl.Source @curlArgs 2>$null
    $code = 0
    $raw = (($out | Out-String).Trim())
    if ($raw -and [int]::TryParse($raw, [ref]$code)) { return $code }
    return 0
}

function Wait-HttpStatus {
    param(
        [Parameter(Mandatory)][string]$Url,
        [string]$Name = "",
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [int]$TimeoutSeconds = 180,
        [int[]]$AcceptStatus = @(200, 201, 202, 204, 301, 302, 307, 308, 400, 401, 403, 404)
    )

    if (-not $Name) { $Name = $Url }
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $attempt = 0
    $last = 0
    while ((Get-Date) -lt $deadline) {
        $attempt++
        $last = Get-HttpStatus -Url $Url -Method $Method -Headers $Headers
        if ($AcceptStatus -contains $last) {
            Write-Host "==> OK    $Name -> HTTP $last"
            return $true
        }
        if ($attempt -eq 1) { Write-Host "==> Esperando $Name ..." }
        Start-Sleep -Seconds 5
    }
    Write-Host "==> FALLA $Name -> HTTP $last"
    return $false
}

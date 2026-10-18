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
    $token | ssh @SshOptions $SshTarget "docker login ghcr.io -u $username --password-stdin"
    if ($LASTEXITCODE -ne 0) {
        throw "docker login ghcr.io fallo en $SshTarget. Revisa GHCR_USERNAME/GHCR_TOKEN (PAT con scope read:packages) o haz el paquete publico."
    }
    Write-Host "==> GHCR login OK en $SshTarget"
}

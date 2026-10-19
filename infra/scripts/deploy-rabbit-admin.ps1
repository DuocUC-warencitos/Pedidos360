#Requires -Version 5.1
<#
.SYNOPSIS
    Despliega rabbit-admin-service en su EC2 via SSH.
.DESCRIPTION
    Sube infra/docker/rabbit-admin/docker-compose.yml + un .env generado desde
    terraform (datasource RDS admin: base logica "admin" creada en la instancia
    RDS de pedidos; IPs privadas) + infra/envs/comun.env (JWT, credenciales
    RabbitMQ). Overrides en infra/envs/rabbit-admin/.env.
.EXAMPLE
    .\deploy-rabbit-admin.ps1
#>
[CmdletBinding()]
param(
    [string]$KeyPath,
    [string]$HostIp,
    [string]$RabbitPrivateIp,
    [string]$SshUser = "ec2-user",
    [string]$TerraformExe,
    [string]$EnvDeployFile
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "_comun.ps1")

$TerraformDir = Join-Path $PSScriptRoot "..\terraform"
$Exe = Get-TerraformExe -Override $TerraformExe
$comun = Initialize-ComunEnv -ScriptRoot $PSScriptRoot -EnvDeployFile $EnvDeployFile
$deploy = $comun.Values
$KeyPath = Assert-SshKey -KeyPath $KeyPath -Deploy $deploy -EnvPath $comun.Path

if (-not $HostIp) { $HostIp = Get-TerraformOutput -Name "ec2_rabbit_admin_public_ip" -Exe $Exe -TerraformDir $TerraformDir }
if (-not $RabbitPrivateIp) { $RabbitPrivateIp = Get-TerraformOutput -Name "ec2_rabbit_private_ip" -Exe $Exe -TerraformDir $TerraformDir }

$dbUrl = Get-TerraformOutput -Name "spring_datasource_url_admin" -Exe $Exe -TerraformDir $TerraformDir
$dbUser = Get-TerraformOutput -Name "spring_datasource_username_pedidos" -Exe $Exe -TerraformDir $TerraformDir
$dbPass = Get-TerraformOutput -Name "spring_datasource_password_pedidos" -Exe $Exe -TerraformDir $TerraformDir
$dbUrlBase = Get-TerraformOutput -Name "spring_datasource_url_pedidos" -Exe $Exe -TerraformDir $TerraformDir

if ($dbUrl -notmatch "jdbc:postgresql://([^:/\?]+):\d+/([^?\s]+)") {
    throw "No se pudo parsear spring_datasource_url_admin: $dbUrl"
}
$dbHost = $Matches[1]
$dbName = $Matches[2]

if ($dbUrlBase -notmatch "jdbc:postgresql://[^:/\?]+:\d+/([^?\s]+)") {
    throw "No se pudo parsear spring_datasource_url_pedidos: $dbUrlBase"
}
$dbInitial = $Matches[1]

$image = if ($deploy["GHCR_IMAGE_RABBIT_ADMIN"]) { $deploy["GHCR_IMAGE_RABBIT_ADMIN"] } else { "ghcr.io/duocuc-warencitos/rabbitmq-admin-service:latest" }

# CORS en AWS: true por defecto (backend); CORS_ALLOWED_ORIGINS sale de comun.env.
# En local los servicios van CORS_ENABLED=false (el gateway local maneja CORS).
$baseLines = @(
    "SERVER_PORT=8080"
    "CORS_ALLOWED_ORIGINS=$($deploy['CORS_ALLOWED_ORIGINS'])"
    "GHCR_IMAGE_RABBIT_ADMIN=$image"
    "SPRING_DATASOURCE_URL=$dbUrl"
    "SPRING_DATASOURCE_USERNAME=$dbUser"
    "SPRING_DATASOURCE_PASSWORD=$dbPass"
    "JWT_ISSUER_URI=$($deploy['JWT_ISSUER_URI'])"
    "RABBITMQ_HOST=$RabbitPrivateIp"
    "RABBITMQ_PORT=5672"
    "RABBITMQ_USERNAME=$($deploy['RABBITMQ_USERNAME'])"
    "RABBITMQ_PASSWORD=$($deploy['RABBITMQ_PASSWORD'])"
    "RABBITMQ_ADMIN_PORT=15672"
    "RABBITMQ_MANAGEMENT_URL=http://${RabbitPrivateIp}:15672"
)

$overrides = Get-ServiceOverrides -ScriptRoot $PSScriptRoot -Service "rabbit-admin"
$envLines = Merge-EnvLines -BaseLines $baseLines -Overrides $overrides

if (-not ($envLines | Where-Object { $_ -match "^CORS_ALLOWED_ORIGINS=.+" })) {
    Write-Host "==> Aviso: CORS_ALLOWED_ORIGINS vacio; el front (Vercel) no podra llamar a rabbit-admin-service."
}

$remoteDir = "/opt/rabbit-admin"
$target = "$SshUser@$HostIp"
$sshOptions = @("-i", $KeyPath, "-o", "StrictHostKeyChecking=accept-new")

Ensure-RemoteDisk -SshTarget $target -SshOptions $sshOptions

$tmpEnv = Join-Path $env:TEMP "rabbit-admin.env"
Write-EnvFile -Path $tmpEnv -Lines $envLines

try {
    Write-Host "==> Preparando $remoteDir en $target"
    ssh @sshOptions $target "sudo mkdir -p $remoteDir && sudo chown -R ec2-user:ec2-user $remoteDir"

    Write-Host "==> Subiendo docker-compose.yml + .env"
    scp @sshOptions "$PSScriptRoot\..\docker\rabbit-admin\docker-compose.yml" "${target}:$remoteDir/docker-compose.yml"
    scp @sshOptions $tmpEnv "${target}:$remoteDir/.env"

    Connect-RemoteGhcr -Deploy $deploy -SshTarget $target -SshOptions $sshOptions

    Write-Host "==> Asegurando base de datos '$dbName' en RDS ($dbHost)"
    $psqlImage = "postgres:15-alpine"
    $pullOut = (ssh @sshOptions $target "docker pull -q $psqlImage" 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) { throw "No se pudo descargar ${psqlImage} en la EC2: $pullOut" }

    $psqlBase = "docker run -i --rm -e PGPASSWORD='$dbPass' $psqlImage psql -h $dbHost -p 5432 -U $dbUser -d postgres -tA"
    $existsOut = (("SELECT 1 FROM pg_database WHERE datname='$dbName';") | ssh @sshOptions $target $psqlBase | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) { throw "psql no pudo conectar a RDS (exit $LASTEXITCODE)" }
    $dbExists = ($existsOut -split "`r?`n" | Where-Object { $_.Trim() -eq "1" }).Count -gt 0
    if (-not $dbExists) {
        Write-Host "==> Creando base de datos '$dbName'"
        ("CREATE DATABASE $dbName;") | ssh @sshOptions $target $psqlBase | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "No se pudo crear la base '$dbName' (exit $LASTEXITCODE)" }
    }

    Write-Host "==> Levantando rabbit-admin-service"
    ssh @sshOptions $target "cd $remoteDir && docker compose up -d && sleep 30 && docker logs --tail 60 rabbit-admin-service"
}
finally {
    Remove-Item -LiteralPath $tmpEnv -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "rabbit-admin-service desplegado. Prueba: http://${HostIp}:8080/api/admin/rabbit/overview"

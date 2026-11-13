#Requires -Version 5.1
<#
.SYNOPSIS
    Despliega analytics-service (Kafka Streams) en su EC2 via SSH (env + compose).
.DESCRIPTION
    Sube infra/docker/analytics-service/docker-compose.yml + un .env generado
    desde terraform (bootstrap privado de los 3 brokers Kafka) + infra/envs/comun.env
    (JWT y CORS).
    Overrides en infra/envs/analytics-service/.env.
    Limpia el stack legado (docker-compose.prod.yml) del user_data si existe.
.EXAMPLE
    .\deploy-analytics-service.ps1
#>
[CmdletBinding()]
param(
    [string]$KeyPath,
    [string]$HostIp,
    [string]$KafkaBootstrapServers,
    [string]$SshUser = "ec2-user",
    [string]$AppDir = "/opt/analytics-service",
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

if (-not $HostIp) { $HostIp = Get-TerraformOutput -Name "ec2_analiticas_public_ip" -Exe $Exe -TerraformDir $TerraformDir }
if (-not $KafkaBootstrapServers) {
    $KafkaBootstrapServers = Get-TerraformOutput -Name "kafka_bootstrap_servers" -Exe $Exe -TerraformDir $TerraformDir
}

$image = if ($deploy["GHCR_IMAGE_ANALYTICS"]) { $deploy["GHCR_IMAGE_ANALYTICS"] } else { "ghcr.io/duocuc-warencitos/analytics-service:latest" }

# CORS en AWS: true por defecto (backend); CORS_ALLOWED_ORIGINS sale de comun.env.
# Kafka Streams usa 3 particiones y RF 3 (coincide con el cluster de 3 brokers).
$baseLines = @(
    "SERVER_PORT=8080"
    "CORS_ALLOWED_ORIGINS=$($deploy['CORS_ALLOWED_ORIGINS'])"
    "GHCR_IMAGE_ANALYTICS=$image"
    "JWT_ISSUER_URI=$($deploy['JWT_ISSUER_URI'])"
    "KAFKA_BOOTSTRAP_SERVERS=$KafkaBootstrapServers"
    "KAFKA_REPLICATION_FACTOR=3"
    "KAFKA_STREAMS_STATE_DIR=/tmp/kafka-streams"
    "JAVA_TOOL_OPTIONS=-Xmx768m"
)

$overrides = Get-ServiceOverrides -ScriptRoot $PSScriptRoot -Service "analytics-service"
$envLines = Merge-EnvLines -BaseLines $baseLines -Overrides $overrides

if (-not ($envLines | Where-Object { $_ -match "^CORS_ALLOWED_ORIGINS=.+" })) {
    Write-Host "==> Aviso: CORS_ALLOWED_ORIGINS vacio; el front (Vercel) no podra llamar a analytics-service."
}
if (-not ($envLines | Where-Object { $_ -match "^KAFKA_BOOTSTRAP_SERVERS=.+" })) {
    Write-Host "==> Aviso: KAFKA_BOOTSTRAP_SERVERS vacio; Kafka Streams no podra consumir los topicos."
}

$target = "$SshUser@$HostIp"
$sshOptions = @("-i", $KeyPath, "-o", "StrictHostKeyChecking=accept-new")

Wait-RemoteSsh -SshTarget $target -SshOptions $sshOptions
Ensure-RemoteDisk -SshTarget $target -SshOptions $sshOptions
Wait-RemoteDocker -SshTarget $target -SshOptions $sshOptions

$tmpEnv = Join-Path $env:TEMP "analytics-service.env"
Write-EnvFile -Path $tmpEnv -Lines $envLines

$remoteScript = @'
set -e
APP_DIR="__APP_DIR__"
cd "$APP_DIR"
if [ -f docker-compose.prod.yml ]; then
  echo "Limpiando stack legado (docker-compose.prod.yml)..."
  docker compose --env-file .env -f docker-compose.prod.yml down --remove-orphans || true
  mv docker-compose.prod.yml docker-compose.prod.yml.legacy
fi
docker compose --env-file .env -f docker-compose.yml pull || echo 'Aviso: pull fallo; se usa la imagen local si existe'
docker compose --env-file .env -f docker-compose.yml up -d --remove-orphans
sleep 20
docker compose --env-file .env -f docker-compose.yml ps
docker logs --tail 30 analytics-service || true
'@

$remoteScript = ($remoteScript.Replace("__APP_DIR__", $AppDir)) -replace "`r", ""

try {
    Write-Host "==> Preparando $AppDir en $target"
    ssh @sshOptions $target "sudo mkdir -p $AppDir && sudo chown -R ec2-user:ec2-user $AppDir"

    Write-Host "==> Subiendo docker-compose.yml + .env"
    scp @sshOptions "$PSScriptRoot\..\docker\analytics-service\docker-compose.yml" "${target}:$AppDir/docker-compose.yml"
    scp @sshOptions $tmpEnv "${target}:$AppDir/.env"

    Connect-RemoteGhcr -Deploy $deploy -SshTarget $target -SshOptions $sshOptions

    Write-Host "==> Levantando analytics-service"
    Invoke-RemoteScript -SshTarget $target -SshOptions $sshOptions -Script $remoteScript
    if ($LASTEXITCODE -ne 0) { throw "docker compose up fallo en analytics-service ($target)." }
}
finally {
    Remove-Item -LiteralPath $tmpEnv -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "analytics-service desplegado. URL: http://${HostIp}:8080/api/v1/admin/analiticas"

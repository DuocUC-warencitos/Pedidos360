#Requires -Version 5.1
<#
.SYNOPSIS
    Despliega pedidos-service en su EC2 via SSH (env completo + compose).
.DESCRIPTION
    Sube infra/docker/pedidos-service/docker-compose.yml + un .env generado desde
    terraform (datasource RDS pedidos, IPs privadas de producto y cluster) +
    infra/envs/comun.env (JWT, credenciales RabbitMQ).
    Overrides en infra/envs/pedidos-service/.env.
    Limpia el stack legado (docker-compose.prod.yml) del user_data si existe.
.EXAMPLE
    .\deploy-pedidos-service.ps1
#>
[CmdletBinding()]
param(
    [string]$KeyPath,
    [string]$HostIp,
    [string]$RabbitPrivateIp,
    [string]$ProductoPrivateIp,
    [string]$SshUser = "ec2-user",
    [string]$AppDir = "/opt/pedidos360",
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

if (-not $HostIp) { $HostIp = Get-TerraformOutput -Name "ec2_pedidos_public_ip" -Exe $Exe -TerraformDir $TerraformDir }
if (-not $RabbitPrivateIp) { $RabbitPrivateIp = Get-TerraformOutput -Name "ec2_rabbit_private_ip" -Exe $Exe -TerraformDir $TerraformDir }
if (-not $ProductoPrivateIp) { $ProductoPrivateIp = Get-TerraformOutput -Name "ec2_producto_private_ip" -Exe $Exe -TerraformDir $TerraformDir }

$dbUrl = Get-TerraformOutput -Name "spring_datasource_url_pedidos" -Exe $Exe -TerraformDir $TerraformDir
$dbUser = Get-TerraformOutput -Name "spring_datasource_username_pedidos" -Exe $Exe -TerraformDir $TerraformDir
$dbPass = Get-TerraformOutput -Name "spring_datasource_password_pedidos" -Exe $Exe -TerraformDir $TerraformDir

$image = if ($deploy["GHCR_IMAGE_PEDIDOS"]) { $deploy["GHCR_IMAGE_PEDIDOS"] } else { "ghcr.io/duocuc-warencitos/pedidos-service:latest" }

$baseLines = @(
    "SERVER_PORT=8080"
    "CORS_ENABLED=false"
    "GHCR_IMAGE_PEDIDOS=$image"
    "SPRING_DATASOURCE_URL=$dbUrl"
    "SPRING_DATASOURCE_USERNAME=$dbUser"
    "SPRING_DATASOURCE_PASSWORD=$dbPass"
    "JWT_ISSUER_URI=$($deploy['JWT_ISSUER_URI'])"
    "PRODUCTO_SERVICE_URL=http://${ProductoPrivateIp}:8080"
    "RABBITMQ_HOST=$RabbitPrivateIp"
    "RABBITMQ_PORT=5672"
    "RABBITMQ_USERNAME=$($deploy['RABBITMQ_USERNAME'])"
    "RABBITMQ_PASSWORD=$($deploy['RABBITMQ_PASSWORD'])"
)

$overrides = Get-ServiceOverrides -ScriptRoot $PSScriptRoot -Service "pedidos-service"
$envLines = Merge-EnvLines -BaseLines $baseLines -Overrides $overrides

$target = "$SshUser@$HostIp"
$sshOptions = @("-i", $KeyPath, "-o", "StrictHostKeyChecking=accept-new")

$tmpEnv = Join-Path $env:TEMP "pedidos-service.env"
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
docker compose --env-file .env -f docker-compose.yml up -d
sleep 20
docker compose --env-file .env -f docker-compose.yml ps
docker logs --tail 30 pedidos-service || true
'@

$remoteScript = ($remoteScript.Replace("__APP_DIR__", $AppDir)) -replace "`r", ""

try {
    Write-Host "==> Preparando $AppDir en $target"
    ssh @sshOptions $target "mkdir -p $AppDir"

    Write-Host "==> Subiendo docker-compose.yml + .env"
    scp @sshOptions "$PSScriptRoot\..\docker\pedidos-service\docker-compose.yml" "${target}:$AppDir/docker-compose.yml"
    scp @sshOptions $tmpEnv "${target}:$AppDir/.env"

    Write-Host "==> Levantando pedidos-service"
    $remoteScript | ssh @sshOptions $target "bash -s"
}
finally {
    Remove-Item -LiteralPath $tmpEnv -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "pedidos-service desplegado. URL: http://${HostIp}:8080/api/v1/pedidos"

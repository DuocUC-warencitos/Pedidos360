#Requires -Version 5.1
<#
.SYNOPSIS
    Despliega producto-service en su EC2 via SSH (env completo + compose).
.DESCRIPTION
    Sube infra/docker/producto-service/docker-compose.yml + un .env generado desde
    terraform (datasource RDS productos) + infra/envs/comun.env (JWT).
    Overrides en infra/envs/producto-service/.env.
    Limpia el stack legado (docker-compose.prod.yml) del user_data si existe.
.EXAMPLE
    .\deploy-producto-service.ps1
#>
[CmdletBinding()]
param(
    [string]$KeyPath,
    [string]$HostIp,
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

if (-not $HostIp) { $HostIp = Get-TerraformOutput -Name "ec2_producto_public_ip" -Exe $Exe -TerraformDir $TerraformDir }

$dbUrl = Get-TerraformOutput -Name "spring_datasource_url_producto" -Exe $Exe -TerraformDir $TerraformDir
$dbUser = Get-TerraformOutput -Name "spring_datasource_username_producto" -Exe $Exe -TerraformDir $TerraformDir
$dbPass = Get-TerraformOutput -Name "spring_datasource_password_producto" -Exe $Exe -TerraformDir $TerraformDir

$image = if ($deploy["GHCR_IMAGE_PRODUCTO"]) { $deploy["GHCR_IMAGE_PRODUCTO"] } else { "ghcr.io/duocuc-warencitos/producto-service:latest" }

# CORS en AWS: true por defecto (backend); CORS_ALLOWED_ORIGINS sale de comun.env.
# En local los servicios van CORS_ENABLED=false (el gateway local maneja CORS).
$baseLines = @(
    "SERVER_PORT=8080"
    "CORS_ALLOWED_ORIGINS=$($deploy['CORS_ALLOWED_ORIGINS'])"
    "GHCR_IMAGE_PRODUCTO=$image"
    "SPRING_DATASOURCE_URL=$dbUrl"
    "SPRING_DATASOURCE_USERNAME=$dbUser"
    "SPRING_DATASOURCE_PASSWORD=$dbPass"
    "JWT_ISSUER_URI=$($deploy['JWT_ISSUER_URI'])"
    "INTERNAL_API_KEY=$($deploy['INTERNAL_API_KEY'])"
)

$overrides = Get-ServiceOverrides -ScriptRoot $PSScriptRoot -Service "producto-service"
$envLines = Merge-EnvLines -BaseLines $baseLines -Overrides $overrides

if (-not ($envLines | Where-Object { $_ -match "^CORS_ALLOWED_ORIGINS=.+" })) {
    Write-Host "==> Aviso: CORS_ALLOWED_ORIGINS vacio; el front (Vercel) no podra llamar a producto-service."
}
if (-not ($envLines | Where-Object { $_ -match "^INTERNAL_API_KEY=.+" })) {
    Write-Host "==> Aviso: INTERNAL_API_KEY vacio; stock/detalles quedaran bloqueados (llamadas internas de pedidos)."
}

$target = "$SshUser@$HostIp"
$sshOptions = @("-i", $KeyPath, "-o", "StrictHostKeyChecking=accept-new")

$tmpEnv = Join-Path $env:TEMP "producto-service.env"
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
docker logs --tail 30 producto-service || true
'@

$remoteScript = ($remoteScript.Replace("__APP_DIR__", $AppDir)) -replace "`r", ""

try {
    Write-Host "==> Preparando $AppDir en $target"
    ssh @sshOptions $target "mkdir -p $AppDir"

    Write-Host "==> Subiendo docker-compose.yml + .env"
    scp @sshOptions "$PSScriptRoot\..\docker\producto-service\docker-compose.yml" "${target}:$AppDir/docker-compose.yml"
    scp @sshOptions $tmpEnv "${target}:$AppDir/.env"

    Connect-RemoteGhcr -Deploy $deploy -SshTarget $target -SshOptions $sshOptions

    Write-Host "==> Levantando producto-service"
    $remoteScript | ssh @sshOptions $target "bash -s"
}
finally {
    Remove-Item -LiteralPath $tmpEnv -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "producto-service desplegado. URL: http://${HostIp}:8080/api/v1/productos"

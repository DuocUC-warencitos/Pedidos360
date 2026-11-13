#Requires -Version 5.1
<#
.SYNOPSIS
    Despliega el cluster RabbitMQ (rabbit1 + rabbit2) en la EC2 via SSH.
.DESCRIPTION
    Sube infra/docker/rabbit-cluster/docker-compose.yml + un .env generado desde
    infra/envs/comun.env (o infra/envs/rabbit-cluster/.env para overrides) y
    levanta los contenedores con docker compose.

    Por defecto borra los volumenes de RabbitMQ (docker compose down -v) para que
    una re-ejecucion deje el cluster limpio y rabbit2 se una de nuevo a rabbit1.
    Usa -KeepData para conservar los volumenes.

    Las IPs se obtienen de terraform output.
.EXAMPLE
    .\deploy-rabbit-cluster.ps1
.EXAMPLE
    .\deploy-rabbit-cluster.ps1 -KeepData
#>
[CmdletBinding()]
param(
    [string]$KeyPath,
    [string]$HostIp,
    [string]$SshUser = "ec2-user",
    [string]$TerraformExe,
    [string]$EnvDeployFile,
    [switch]$KeepData
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "_comun.ps1")

$TerraformDir = Join-Path $PSScriptRoot "..\terraform"
$Exe = Get-TerraformExe -Override $TerraformExe
$comun = Initialize-ComunEnv -ScriptRoot $PSScriptRoot -EnvDeployFile $EnvDeployFile
$deploy = $comun.Values
$KeyPath = Assert-SshKey -KeyPath $KeyPath -Deploy $deploy -EnvPath $comun.Path

if (-not $HostIp) { $HostIp = Get-TerraformOutput -Name "ec2_rabbit_public_ip" -Exe $Exe -TerraformDir $TerraformDir }

$overrides = Get-ServiceOverrides -ScriptRoot $PSScriptRoot -Service "rabbit-cluster"

$baseLines = @(
    "RABBITMQ_USERNAME=$($deploy['RABBITMQ_USERNAME'])"
    "RABBITMQ_PASSWORD=$($deploy['RABBITMQ_PASSWORD'])"
    "RABBITMQ_ERLANG_COOKIE=$($deploy['RABBITMQ_ERLANG_COOKIE'])"
)

$envLines = Merge-EnvLines -BaseLines $baseLines -Overrides $overrides

$remoteDir = "/opt/rabbit-cluster"
$target = "$SshUser@$HostIp"
$sshOptions = @("-i", $KeyPath, "-o", "StrictHostKeyChecking=accept-new")

Wait-RemoteSsh -SshTarget $target -SshOptions $sshOptions
Ensure-RemoteDisk -SshTarget $target -SshOptions $sshOptions
Wait-RemoteDocker -SshTarget $target -SshOptions $sshOptions

$tmpEnv = Join-Path $env:TEMP "rabbit-cluster.env"
Write-EnvFile -Path $tmpEnv -Lines $envLines

$wipe = if ($KeepData) { "" } else { "docker compose down -v --remove-orphans >/dev/null 2>&1 || true" }

try {
    Write-Host "==> Preparando $remoteDir en $target"
    ssh @sshOptions $target "sudo mkdir -p $remoteDir && sudo chown -R ec2-user:ec2-user $remoteDir"
    if ($LASTEXITCODE -ne 0) { throw "No se pudo preparar $remoteDir en $target." }

    Write-Host "==> Subiendo docker-compose.yml + .env"
    scp @sshOptions "$PSScriptRoot\..\docker\rabbit-cluster\docker-compose.yml" "${target}:$remoteDir/docker-compose.yml"
    if ($LASTEXITCODE -ne 0) { throw "Fallo scp de docker-compose.yml a $target." }
    scp @sshOptions $tmpEnv "${target}:$remoteDir/.env"
    if ($LASTEXITCODE -ne 0) { throw "Fallo scp de .env a $target." }

    Write-Host "==> Levantando cluster (rabbit1 + rabbit2)"
    $remote = @"
set -e
cd $remoteDir
$wipe
docker compose pull || echo 'Aviso: pull fallo; se usa la imagen local si existe'
docker compose up -d --remove-orphans
echo '==> Esperando a que rabbit2 este running en el cluster...'
for i in `$(seq 1 24); do
  if docker exec rabbit1 rabbitmqctl eval 'rabbit_nodes:list_running().' 2>/dev/null | grep -q 'rabbit@rabbit2'; then break; fi
  sleep 5
done
docker compose ps
docker exec rabbit1 rabbitmqctl cluster_status
"@
    Invoke-RemoteScript -SshTarget $target -SshOptions $sshOptions -Script $remote
    if ($LASTEXITCODE -ne 0) { throw "docker compose up fallo en el cluster RabbitMQ ($target)." }
}
finally {
    Remove-Item -LiteralPath $tmpEnv -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "Cluster desplegado. Management UI: http://${HostIp}:15672"
Write-Host "Usuario/password: infra\envs\comun.env (RABBITMQ_USERNAME / RABBITMQ_PASSWORD)"

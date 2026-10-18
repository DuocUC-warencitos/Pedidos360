#Requires -Version 5.1
<#
.SYNOPSIS
    Despliega el cluster RabbitMQ (rabbit1 + rabbit2) en la EC2 via SSH.
.DESCRIPTION
    Sube infra/docker/rabbit-cluster/docker-compose.yml + un .env generado desde
    infra/envs/comun.env (o infra/envs/rabbit-cluster/.env para overrides) y
    levanta los contenedores con docker compose.
    Las IPs se obtienen de terraform output.
.EXAMPLE
    .\deploy-rabbit-cluster.ps1
#>
[CmdletBinding()]
param(
    [string]$KeyPath,
    [string]$HostIp,
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

$tmpEnv = Join-Path $env:TEMP "rabbit-cluster.env"
Write-EnvFile -Path $tmpEnv -Lines $envLines

try {
    Write-Host "==> Preparando $remoteDir en $target"
    ssh @sshOptions $target "mkdir -p $remoteDir"

    Write-Host "==> Subiendo docker-compose.yml + .env"
    scp @sshOptions "$PSScriptRoot\..\docker\rabbit-cluster\docker-compose.yml" "${target}:$remoteDir/docker-compose.yml"
    scp @sshOptions $tmpEnv "${target}:$remoteDir/.env"

    Write-Host "==> Levantando cluster (rabbit1 + rabbit2)"
    ssh @sshOptions $target "cd $remoteDir && docker compose up -d && sleep 30 && docker exec rabbit1 rabbitmqctl cluster_status"
}
finally {
    Remove-Item -LiteralPath $tmpEnv -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "Cluster desplegado. Management UI: http://${HostIp}:15672"
Write-Host "Usuario/password: infra\envs\comun.env (RABBITMQ_USERNAME / RABBITMQ_PASSWORD)"

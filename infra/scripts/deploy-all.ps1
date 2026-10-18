#Requires -Version 5.1
<#
.SYNOPSIS
    Levanta TODA la infraestructura Pedidos360: terraform apply + despliegue de las 4 EC2.
.DESCRIPTION
    1. terraform init (si hace falta) + terraform apply (RDS x2, EC2 pedidos, producto,
       rabbit cluster y rabbit-admin).
    2. Despliega por SSH, en orden: cluster RabbitMQ -> rabbit-admin-service ->
       producto-service -> pedidos-service (sube compose + .env generado).
    3. Imprime un resumen con URLs y connection strings (sin passwords).

    Secretos: si infra/envs/comun.env no existe se crea desde el ejemplo y se
    generan automaticamente RABBITMQ_PASSWORD y RABBITMQ_ERLANG_COOKIE.
    Editar infra/envs/comun.env para fijar AWS_KEY_PATH y lo demas.
.EXAMPLE
    .\deploy-all.ps1 -AutoApprove
.EXAMPLE
    .\deploy-all.ps1 -SkipTerraform
#>
[CmdletBinding()]
param(
    [switch]$SkipTerraform,
    [switch]$SkipAwsPrompt,
    [switch]$AutoApprove,
    [string]$TerraformExe,
    [string]$KeyPath,
    [string]$SshUser = "ec2-user",
    [string]$EnvDeployFile
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "_comun.ps1")

$TerraformDir = Join-Path $PSScriptRoot "..\terraform"
$Exe = Get-TerraformExe -Override $TerraformExe
$comun = Initialize-ComunEnv -ScriptRoot $PSScriptRoot -EnvDeployFile $EnvDeployFile
$deploy = $comun.Values
$KeyPath = Assert-SshKey -KeyPath $KeyPath -Deploy $deploy -EnvPath $comun.Path

if (-not $SkipTerraform) {
    Initialize-AwsCredentials -ScriptRoot $PSScriptRoot -SkipPrompt:$SkipAwsPrompt

    if (-not (Test-Path -LiteralPath (Join-Path $TerraformDir ".terraform"))) {
        Write-Host "==> terraform init"
        Invoke-Terraform -Exe $Exe -TerraformDir $TerraformDir -Arguments @("init", "-input=false")
    }

    Write-Host "==> terraform apply"
    if ($AutoApprove) {
        Invoke-Terraform -Exe $Exe -TerraformDir $TerraformDir -Arguments @("apply", "-input=false", "-auto-approve")
    }
    else {
        Invoke-Terraform -Exe $Exe -TerraformDir $TerraformDir -Arguments @("apply")
    }

    Write-Host ""
    Write-Host "NOTA: si es un entorno nuevo, RDS tarda ~10 minutos en quedar disponible."
    Write-Host "      Si algun servicio falla al conectar, espera y re-ejecuta: .\deploy-all.ps1 -SkipTerraform"
    Write-Host ""
}

$scriptArgs = @{ EnvDeployFile = $comun.Path; TerraformExe = $Exe; SshUser = $SshUser }
if ($KeyPath) { $scriptArgs["KeyPath"] = $KeyPath }

Write-Host "==> [1/4] Cluster RabbitMQ (rabbit1 + rabbit2)"
& (Join-Path $PSScriptRoot "deploy-rabbit-cluster.ps1") @scriptArgs

Write-Host ""
Write-Host "==> [2/4] rabbit-admin-service"
& (Join-Path $PSScriptRoot "deploy-rabbit-admin.ps1") @scriptArgs

Write-Host ""
Write-Host "==> [3/4] producto-service"
& (Join-Path $PSScriptRoot "deploy-producto-service.ps1") @scriptArgs

Write-Host ""
Write-Host "==> [4/4] pedidos-service"
& (Join-Path $PSScriptRoot "deploy-pedidos-service.ps1") @scriptArgs

Write-Host ""
Write-Host "================= RESUMEN ================="

$apigwUrl = Get-TerraformOutput -Name "apigw_url" -Exe $Exe -TerraformDir $TerraformDir
$rabbitIp = Get-TerraformOutput -Name "ec2_rabbit_public_ip" -Exe $Exe -TerraformDir $TerraformDir
$rabbitAdminIp = Get-TerraformOutput -Name "ec2_rabbit_admin_public_ip" -Exe $Exe -TerraformDir $TerraformDir
$pedidosIp = Get-TerraformOutput -Name "ec2_pedidos_public_ip" -Exe $Exe -TerraformDir $TerraformDir
$productoIp = Get-TerraformOutput -Name "ec2_producto_public_ip" -Exe $Exe -TerraformDir $TerraformDir
$jdbcPedidos = Get-TerraformOutput -Name "spring_datasource_url_pedidos" -Exe $Exe -TerraformDir $TerraformDir
$jdbcProducto = Get-TerraformOutput -Name "spring_datasource_url_producto" -Exe $Exe -TerraformDir $TerraformDir

$rows = [ordered]@{
    "API Gateway"          = $apigwUrl
    "Rabbit Management"    = "http://${rabbitIp}:15672"
    "rabbit-admin-service" = "http://${rabbitAdminIp}:8080"
    "pedidos-service"      = "http://${pedidosIp}:8080"
    "producto-service"     = "http://${productoIp}:8080"
    "RDS pedidos (jdbc)"   = $jdbcPedidos
    "RDS productos (jdbc)" = $jdbcProducto
}

foreach ($key in $rows.Keys) {
    "{0,-22} {1}" -f $key, $rows[$key]
}

Write-Host ""
Write-Host "Credenciales RabbitMQ y key SSH: $($comun.Path)"
Write-Host "Overrides por servicio: infra\envs\<servicio>\.env (copiar desde .env.example)"

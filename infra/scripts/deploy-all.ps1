#Requires -Version 5.1
<#
.SYNOPSIS
    Levanta TODA la infraestructura Pedidos360: terraform apply + despliegue de las EC2.
.DESCRIPTION
    1. terraform init (si hace falta) + terraform apply (RDS x2, EC2 pedidos, producto,
       rabbit cluster, rabbit-admin, cluster Kafka de 3 brokers y analytics-service).
    2. Despliega por SSH, en orden: cluster Kafka -> cluster RabbitMQ ->
       rabbit-admin-service -> producto-service -> pedidos-service ->
       analytics-service (sube compose + .env generado).
    3. Imprime un resumen con URLs y connection strings (sin passwords).

    Por defecto los clusters Kafka y RabbitMQ se recrean desde cero (se borran
    sus volumenes) para que una re-ejecucion deje siempre un estado limpio.
    Usa -KeepData para conservar los volumenes de Kafka/RabbitMQ.

    Secretos: si infra/envs/comun.env no existe se crea desde el ejemplo y se
    generan automaticamente RABBITMQ_PASSWORD, RABBITMQ_ERLANG_COOKIE,
    INTERNAL_API_KEY y KAFKA_CLUSTER_ID.
    Editar infra/envs/comun.env para fijar AWS_KEY_PATH y lo demas.
.EXAMPLE
    .\deploy-all.ps1 -AutoApprove
.EXAMPLE
    .\deploy-all.ps1 -SkipTerraform
.EXAMPLE
    .\deploy-all.ps1 -SkipTerraform -KeepData
.EXAMPLE
    .\deploy-all.ps1 -SkipKafka
#>
[CmdletBinding()]
param(
    [switch]$SkipTerraform,
    [switch]$SkipAwsPrompt,
    [switch]$AutoApprove,
    [switch]$SkipKafka,
    [switch]$KeepData,
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

if (-not $SkipKafka) {
    Write-Host "==> [1/6] Cluster Kafka (3 brokers KRaft)"
    $kafkaArgs = @{} + $scriptArgs
    if ($KeepData) { $kafkaArgs["KeepData"] = $true }
    & (Join-Path $PSScriptRoot "deploy-kafka-cluster.ps1") @kafkaArgs
    Write-Host ""
}

Write-Host "==> [2/6] Cluster RabbitMQ (rabbit1 + rabbit2)"
$rabbitArgs = @{} + $scriptArgs
if ($KeepData) { $rabbitArgs["KeepData"] = $true }
& (Join-Path $PSScriptRoot "deploy-rabbit-cluster.ps1") @rabbitArgs

Write-Host ""
Write-Host "==> [3/6] rabbit-admin-service"
& (Join-Path $PSScriptRoot "deploy-rabbit-admin.ps1") @scriptArgs

Write-Host ""
Write-Host "==> [4/6] producto-service"
& (Join-Path $PSScriptRoot "deploy-producto-service.ps1") @scriptArgs

Write-Host ""
Write-Host "==> [5/6] pedidos-service"
& (Join-Path $PSScriptRoot "deploy-pedidos-service.ps1") @scriptArgs

Write-Host ""
Write-Host "==> [6/6] analytics-service (Kafka Streams)"
& (Join-Path $PSScriptRoot "deploy-analytics-service.ps1") @scriptArgs

Write-Host ""
Write-Host "================= RESUMEN ================="

$apigwUrl = Get-TerraformOutput -Name "apigw_url" -Exe $Exe -TerraformDir $TerraformDir
$rabbitIp = Get-TerraformOutput -Name "ec2_rabbit_public_ip" -Exe $Exe -TerraformDir $TerraformDir
$rabbitAdminIp = Get-TerraformOutput -Name "ec2_rabbit_admin_public_ip" -Exe $Exe -TerraformDir $TerraformDir
$pedidosIp = Get-TerraformOutput -Name "ec2_pedidos_public_ip" -Exe $Exe -TerraformDir $TerraformDir
$productoIp = Get-TerraformOutput -Name "ec2_producto_public_ip" -Exe $Exe -TerraformDir $TerraformDir
$analiticasIp = Get-TerraformOutput -Name "ec2_analiticas_public_ip" -Exe $Exe -TerraformDir $TerraformDir
$jdbcPedidos = Get-TerraformOutput -Name "spring_datasource_url_pedidos" -Exe $Exe -TerraformDir $TerraformDir
$jdbcProducto = Get-TerraformOutput -Name "spring_datasource_url_producto" -Exe $Exe -TerraformDir $TerraformDir

$rows = [ordered]@{
    "API Gateway"          = $apigwUrl
    "Rabbit Management"    = "http://${rabbitIp}:15672"
    "rabbit-admin-service" = "http://${rabbitAdminIp}:8080"
    "pedidos-service"      = "http://${pedidosIp}:8080"
    "producto-service"     = "http://${productoIp}:8080"
    "analytics-service"    = "http://${analiticasIp}:8080"
    "RDS pedidos (jdbc)"   = $jdbcPedidos
    "RDS productos (jdbc)" = $jdbcProducto
}

if (-not $SkipKafka) {
    $rows["Kafka UI"] = Get-TerraformOutput -Name "kafka_ui_url" -Exe $Exe -TerraformDir $TerraformDir
    $rows["Kafka bootstrap"] = Get-TerraformOutput -Name "kafka_bootstrap_servers" -Exe $Exe -TerraformDir $TerraformDir
}

foreach ($key in $rows.Keys) {
    "{0,-22} {1}" -f $key, $rows[$key]
}

Write-Host ""
Write-Host "Credenciales RabbitMQ y key SSH: $($comun.Path)"
Write-Host "Overrides por servicio: infra\envs\<servicio>\.env (copiar desde .env.example)"

Write-Host ""
Write-Host "==> Verificando servicios (health directo en cada EC2)"

$healthChecks = @(
    @{ Name = "pedidos-service"; Url = "http://${pedidosIp}:8080/api/v1/pedidos" }
    @{ Name = "producto-service"; Url = "http://${productoIp}:8080/api/v1/productos" }
    @{ Name = "rabbit-admin-service"; Url = "http://${rabbitAdminIp}:8080/api/admin/rabbit/overview" }
    @{ Name = "analytics-service"; Url = "http://${analiticasIp}:8080/actuator/health"; Accept = @(200) }
)

$failed = @()
foreach ($check in $healthChecks) {
    # 401/403 tambien valen: el servicio esta arriba y responde (JWT obligatorio).
    $accept = if ($check.ContainsKey("Accept")) { $check.Accept } else { @(200, 401, 403) }
    if (-not (Wait-HttpStatus -Url $check.Url -Name $check.Name -TimeoutSeconds 180 -AcceptStatus $accept)) {
        $failed += $check.Name
    }
}

Write-Host ""
Write-Host "==> Verificando preflight CORS en API Gateway ($apigwUrl)"
$corsOrigins = if ($deploy["CORS_ALLOWED_ORIGINS"]) { $deploy["CORS_ALLOWED_ORIGINS"] } else { "https://pedidos360-three.vercel.app" }
$origin = ($corsOrigins -split ",")[0].Trim()
$preflightHeaders = @{
    "Origin"                        = $origin
    "Access-Control-Request-Method" = "GET"
    "Access-Control-Request-Headers" = "authorization,content-type"
}
foreach ($path in @("/api/v1/productos", "/api/v1/pedidos", "/api/v1/admin/pedidos", "/api/v1/admin/productos", "/api/v1/admin/analiticas/inventario/top")) {
    $ok = Wait-HttpStatus -Url "$apigwUrl$path" -Name "OPTIONS $path" -Method "OPTIONS" -Headers $preflightHeaders -TimeoutSeconds 60 -AcceptStatus @(200, 204)
    if (-not $ok) { $failed += "OPTIONS $path" }
}

if ($failed.Count -gt 0) {
    throw "Deploy termino pero la verificacion fallo: $($failed -join ', '). Revisa 'docker compose logs' en las EC2."
}

Write-Host ""
Write-Host "Todo verificado: servicios arriba y preflight CORS OK."

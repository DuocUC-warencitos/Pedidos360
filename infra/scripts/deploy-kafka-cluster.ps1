#Requires -Version 5.1
<#
.SYNOPSIS
    Despliega el cluster Kafka KRaft de 3 brokers en 3 EC2 de la misma subnet/AZ.
.DESCRIPTION
    Sube infra/docker/kafka-cluster/docker-compose.yml + init-topics.sh + un .env
    por broker (node id, IP privada, quorum de controllers y CLUSTER_ID) y levanta
    Kafka UI en el broker indicado (por defecto kafka3).
    Luego espera el quorum y crea los topicos con 3 particiones, RF 3 y min ISR 2.
    El CLUSTER_ID se genera una sola vez y se guarda en infra/envs/comun.env.
.EXAMPLE
    .\deploy-kafka-cluster.ps1
.EXAMPLE
    .\deploy-kafka-cluster.ps1 -UiNode 2
.EXAMPLE
    .\deploy-kafka-cluster.ps1 -SkipUi -SkipTopicInit
#>
[CmdletBinding()]
param(
    [string]$KeyPath,
    [string]$SshUser = "ec2-user",
    [string]$TerraformExe,
    [string]$EnvDeployFile,
    [ValidateRange(1, 3)][int]$UiNode = 3,
    [switch]$SkipUi,
    [switch]$SkipTopicInit
)

$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "_comun.ps1")

$TerraformDir = Join-Path $PSScriptRoot "..\terraform"
$Exe = Get-TerraformExe -Override $TerraformExe
$comun = Initialize-ComunEnv -ScriptRoot $PSScriptRoot -EnvDeployFile $EnvDeployFile
$deploy = $comun.Values
$KeyPath = Assert-SshKey -KeyPath $KeyPath -Deploy $deploy -EnvPath $comun.Path

$clusterId = $deploy["KAFKA_CLUSTER_ID"]
if (-not $clusterId) {
    throw "KAFKA_CLUSTER_ID vacio en $($comun.Path). Revisa Initialize-ComunEnv o define la clave manualmente."
}

$privateIps = @(Get-TerraformOutputJson -Name "ec2_kafka_private_ips" -Exe $Exe -TerraformDir $TerraformDir)
$publicIps = @(Get-TerraformOutputJson -Name "ec2_kafka_public_ips" -Exe $Exe -TerraformDir $TerraformDir)
if ($privateIps.Count -ne 3 -or $publicIps.Count -ne 3) {
    throw "Se esperaban 3 brokers Kafka; terraform devolvio $($privateIps.Count) IPs privadas y $($publicIps.Count) publicas."
}

$bootstrapServers = ($privateIps | ForEach-Object { "$($_):9092" }) -join ","
$quorumVoters = (1..3 | ForEach-Object { "$_@$($privateIps[$_ - 1]):9093" }) -join ","

$overrides = Get-ServiceOverrides -ScriptRoot $PSScriptRoot -Service "kafka-cluster"
# Estos valores son por nodo y no se deben pisar desde el .env de overrides.
foreach ($protectedKey in @("KAFKA_NODE_ID", "BROKER_PRIVATE_IP", "QUORUM_VOTERS", "KAFKA_CLUSTER_ID", "BOOTSTRAP_SERVERS")) {
    if ($overrides.ContainsKey($protectedKey)) {
        Write-Host "==> Ignorando override $protectedKey (lo calcula el script por nodo)."
        $overrides.Remove($protectedKey)
    }
}

$remoteDir = "/opt/kafka"
$composePath = Join-Path $PSScriptRoot "..\docker\kafka-cluster\docker-compose.yml"
$initScriptPath = Join-Path $PSScriptRoot "..\docker\kafka-cluster\init-topics.sh"

for ($i = 0; $i -lt 3; $i++) {
    $nodeId = $i + 1
    $target = "$SshUser@$($publicIps[$i])"
    $sshOptions = @("-i", $KeyPath, "-o", "StrictHostKeyChecking=accept-new")

    Write-Host ""
    Write-Host "==> Kafka broker $nodeId ($target, $($privateIps[$i]))"

    $baseLines = @(
        "KAFKA_NODE_ID=$nodeId"
        "BROKER_PRIVATE_IP=$($privateIps[$i])"
        "QUORUM_VOTERS=$quorumVoters"
        "KAFKA_CLUSTER_ID=$clusterId"
        "BOOTSTRAP_SERVERS=$bootstrapServers"
        "KAFKA_HEAP_OPTS=-Xms512m -Xmx1g"
        "UI_PORT=8080"
    )
    $envLines = Merge-EnvLines -BaseLines $baseLines -Overrides $overrides

    Ensure-RemoteDisk -SshTarget $target -SshOptions $sshOptions

    $tmpEnv = Join-Path $env:TEMP "kafka-broker-$nodeId.env"
    Write-EnvFile -Path $tmpEnv -Lines $envLines

    try {
        Write-Host "==> Preparando $remoteDir en $target"
        ssh @sshOptions $target "sudo mkdir -p $remoteDir && sudo chown -R ec2-user:ec2-user $remoteDir"
        if ($LASTEXITCODE -ne 0) { throw "No se pudo preparar $remoteDir en $target." }

        Write-Host "==> Subiendo compose, init-topics y .env"
        scp @sshOptions $composePath "${target}:$remoteDir/docker-compose.yml"
        if ($LASTEXITCODE -ne 0) { throw "Fallo scp de docker-compose.yml a $target." }
        scp @sshOptions $initScriptPath "${target}:$remoteDir/init-topics.sh"
        if ($LASTEXITCODE -ne 0) { throw "Fallo scp de init-topics.sh a $target." }
        scp @sshOptions $tmpEnv "${target}:$remoteDir/.env"
        if ($LASTEXITCODE -ne 0) { throw "Fallo scp de .env a $target." }

        $profileArgs = if (-not $SkipUi -and $nodeId -eq $UiNode) { "--profile ui" } else { "" }
        Write-Host "==> Levantando Kafka broker $nodeId"
        ssh @sshOptions $target "cd $remoteDir && docker compose --env-file .env $profileArgs up -d"
        if ($LASTEXITCODE -ne 0) { throw "docker compose up fallo en el broker $nodeId ($target)." }
    }
    finally {
        Remove-Item -LiteralPath $tmpEnv -Force -ErrorAction SilentlyContinue
    }
}

if (-not $SkipTopicInit) {
    $target = "$SshUser@$($publicIps[0])"
    $sshOptions = @("-i", $KeyPath, "-o", "StrictHostKeyChecking=accept-new")

    Write-Host ""
    Write-Host "==> Esperando quorum KRaft (puede tardar 1-2 minutos)"
    $ready = $false
    for ($attempt = 1; $attempt -le 30; $attempt++) {
        ssh @sshOptions $target "docker exec kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 >/dev/null 2>&1" 2>$null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Write-Host "    intento $attempt/30: cluster aun no responde..."
        Start-Sleep -Seconds 10
    }

    if (-not $ready) {
        Write-Host "==> Logs de kafka1:"
        ssh @sshOptions $target "docker logs --tail 80 kafka" 2>&1 | Write-Host
        throw "Kafka no quedo listo en 5 minutos. Revisa los logs de kafka1."
    }

    Write-Host "==> Creando topicos"
    $initOutput = ssh @sshOptions $target "docker exec kafka bash /opt/kafka/init-topics.sh" 2>&1
    $initOutput | Write-Host
    if ($LASTEXITCODE -ne 0) {
        throw "Fallo la creacion de topicos. Revisa la salida anterior."
    }
}

Write-Host ""
Write-Host "Cluster Kafka desplegado."
Write-Host "Bootstrap interno : $bootstrapServers"
Write-Host "Brokers privados  : $($privateIps -join ', ')"
if (-not $SkipUi) {
    Write-Host "Kafka UI          : http://$($publicIps[$UiNode - 1]):8080 (solo desde admin_cidr)"
}

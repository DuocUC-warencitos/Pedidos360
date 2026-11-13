# infra/scripts

Scripts PowerShell de despliegue. Todos cargan `_comun.ps1` para leer
`infra/envs/comun.env`, resolver la key SSH, invocar Terraform y subir los
`docker-compose.yml` + `.env` por SSH.

## Requisitos

- PowerShell 5.1+
- Terraform en PATH (o `-TerraformExe`)
- `ssh`/`scp` de OpenSSH
- `infra/envs/comun.env` con `AWS_KEY_PATH`
- `terraform apply` ejecutado (salvo que uses `deploy-all.ps1`)

## Scripts

| Script | Qué hace |
|---|---|
| `deploy-all.ps1` | `terraform apply` + Kafka + Rabbit + servicios |
| `deploy-kafka-cluster.ps1` | cluster Kafka KRaft de 3 brokers + tópicos + UI |
| `deploy-rabbit-cluster.ps1` | `rabbit1` + `rabbit2` en una EC2 |
| `deploy-rabbit-admin.ps1` | `rabbit-admin-service` |
| `deploy-producto-service.ps1` | `producto-service` |
| `deploy-pedidos-service.ps1` | `pedidos-service` |

## `deploy-all.ps1`

```powershell
.\infra\scripts\deploy-all.ps1 -AutoApprove
```

Parámetros:

| Parámetro | Uso |
|---|---|
| `-AutoApprove` | `terraform apply -auto-approve` |
| `-SkipTerraform` | no ejecuta Terraform; solo despliega |
| `-SkipKafka` | no despliega el cluster Kafka |
| `-SkipAwsPrompt` | no pide credenciales AWS |
| `-KeyPath` | ruta al `.pem` (si no, usa `AWS_KEY_PATH`) |
| `-SshUser` | usuario SSH (default `ec2-user`) |
| `-EnvDeployFile` | `.env` alternativo a `infra/envs/comun.env` |

Orden: Terraform → Kafka → Rabbit → rabbit-admin → producto → pedidos.

## `deploy-kafka-cluster.ps1`

```powershell
# 3 brokers + Kafka UI en kafka3 + tópicos
.\infra\scripts\deploy-kafka-cluster.ps1

# UI en kafka2
.\infra\scripts\deploy-kafka-cluster.ps1 -UiNode 2

# Solo brokers (sin UI ni tópicos)
.\infra\scripts\deploy-kafka-cluster.ps1 -SkipUi -SkipTopicInit
```

Qué hace:

1. Lee `ec2_kafka_private_ips` y `ec2_kafka_public_ips` de Terraform.
2. Genera el `.env` de cada broker con `KAFKA_NODE_ID`, `BROKER_PRIVATE_IP`,
   `QUORUM_VOTERS`, `KAFKA_CLUSTER_ID` y `BOOTSTRAP_SERVERS`.
3. Sube `docker-compose.yml`, `init-topics.sh` y `.env` a `/opt/kafka`.
4. Levanta el broker (y Kafka UI en el nodo indicado).
5. Espera el quórum y ejecuta `init-topics.sh` en kafka1.

`KAFKA_CLUSTER_ID` se genera automáticamente en `infra/envs/comun.env` la primera
vez. **No lo cambies** después del primer arranque.

Overrides opcionales: `infra/envs/kafka-cluster/.env` (copiar desde
`.env.example`). No pongas ahí `KAFKA_NODE_ID`, `BROKER_PRIVATE_IP`,
`QUORUM_VOTERS`, `KAFKA_CLUSTER_ID` ni `BOOTSTRAP_SERVERS`: el script
los calcula por nodo y los ignora si los encuentra.

## `_comun.ps1`

Funciones compartidas:

- `Initialize-ComunEnv`: crea/lee `comun.env` y genera secretos faltantes
  (`RABBITMQ_PASSWORD`, `RABBITMQ_ERLANG_COOKIE`, `INTERNAL_API_KEY`,
  `KAFKA_CLUSTER_ID`).
- `Get-TerraformOutput` / `Get-TerraformOutputJson`: leen outputs.
- `Assert-SshKey`, `Ensure-RemoteDisk`, `Connect-RemoteGhcr`.
- `Write-EnvFile`, `Merge-EnvLines`, `Get-ServiceOverrides`.
- `New-KafkaClusterId`: UUID base64 URL-safe de 22 caracteres para KRaft.

## Notas

- Los scripts no borran datos de Kafka. Para reiniciar el cluster desde cero:
  `docker compose down -v` en cada broker (pierde los logs).
- Si Terraform recrea una EC2 de Kafka, su IP privada cambia. Vuelve a ejecutar
  `deploy-kafka-cluster.ps1` para regenerar los `.env` de los 3 nodos.
- Los secretos quedan en `infra/envs/comun.env` (gitignored).

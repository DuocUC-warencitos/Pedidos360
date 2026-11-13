# Infraestructura Pedidos360

Documentación de la carpeta `infra/`: Terraform, Docker Compose, variables de
despliegue y scripts PowerShell para levantar Pedidos360 en AWS (Learner Lab).

> **KRaft:** la pauta EP6 menciona ZooKeeper, pero el docente autorizó usar KRaft.
> El cluster de este repositorio corre **Kafka 4.2.2 en modo KRaft**, sin ZooKeeper.
> El plan funcional de `docs/` puede seguir mencionando ZooKeeper; esta carpeta es
> la fuente de verdad de la infraestructura implementada.

## Arquitectura

```
Local (docker compose)
  Angular -> Gateway -> pedidos-service -> producto-service
                       -> rabbit-admin-service -> RabbitMQ
                       -> analytics-service -> Kafka (1 broker, dev)

AWS (Learner Lab)
  API Gateway (JWT Entra ID)
    -> EC2 pedidos-service      + RDS pedidos
    -> EC2 producto-service     + RDS productos  --produce--> Kafka
    -> EC2 rabbit-admin-service + RabbitMQ cluster
    -> EC2 analytics-service    <--consume-- Kafka (Kafka Streams)

  Kafka: 3 x EC2 t3.small, misma subnet/AZ, KRaft combinado
         RF=3, min.insync.replicas=2, 3 particiones por tópico
```

## Estructura

```
infra/
├── README.md                  <- este archivo
├── docker/
│   ├── README.md              <- stack local y compose del cluster Kafka
│   ├── docker-compose.yml     <- stack local completo (1 broker Kafka, dev)
│   └── kafka-cluster/         <- cluster Kafka cloud (1 broker por EC2)
│       ├── README.md
│       ├── docker-compose.yml
│       └── init-topics.sh
├── envs/
│   ├── README.md              <- esquema de .env y secretos
│   ├── comun.env.example      <- secretos compartidos + KAFKA_CLUSTER_ID
│   └── kafka-cluster/
│       └── .env.example       <- overrides opcionales del cluster Kafka
├── scripts/
│   ├── README.md              <- guía de scripts
│   ├── deploy-all.ps1
│   ├── deploy-kafka-cluster.ps1
│   ├── deploy-rabbit-cluster.ps1
│   ├── deploy-rabbit-admin.ps1
│   ├── deploy-producto-service.ps1
│   └── deploy-pedidos-service.ps1
└── terraform/
    ├── README.md
    ├── ec2_kafka.tf           <- 3 EC2 t3.small (misma subnet/AZ)
    ├── security_kafka.tf      <- SG de brokers + Kafka UI
    ├── security_analiticas.tf <- SG preparado para analytics-service
    └── ...
```

## Requisitos

- AWS Academy Learner Lab con credenciales vigentes.
- Terraform >= 1.7.
- Docker Desktop (para probar el compose en local).
- PowerShell 5.1+.
- Key pair del lab (`vockey`) y su `.pem` descargado.
- `infra/envs/comun.env` (se crea solo desde `comun.env.example`).
- `terraform.tfvars` (copiar desde `terraform.tfvars.example`).

## Despliegue rápido

```powershell
# 1. Configurar secretos y key SSH
Copy-Item infra\envs\comun.env.example infra\envs\comun.env
notepad infra\envs\comun.env

# 2. Configurar variables de Terraform
Copy-Item infra\terraform\terraform.tfvars.example infra\terraform\terraform.tfvars
notepad infra\terraform\terraform.tfvars

# 3. Todo: terraform apply + Kafka + Rabbit + servicios
.\infra\scripts\deploy-all.ps1 -AutoApprove
```

Sin Kafka (por ejemplo, si solo quieres Rabbit):

```powershell
.\infra\scripts\deploy-all.ps1 -SkipKafka
```

Solo Kafka (Terraform ya aplicado):

```powershell
.\infra\scripts\deploy-kafka-cluster.ps1
```

## Cluster Kafka (KRaft, 3 brokers)

### Topología

| Nodo | EC2 | Rol | Puerto cliente | Controller |
|---|---|---|---|---|
| kafka1 | `t3.small` | broker + controller | 9092 | 9093 |
| kafka2 | `t3.small` | broker + controller | 9092 | 9093 |
| kafka3 | `t3.small` | broker + controller | 9092 | 9093 |

- Las 3 EC2 van en **la misma subnet y AZ** (`local.public_subnet_ids[0]`).
- 30 GB gp3 por broker.
- Kafka UI corre en el broker 3 por defecto (`-UiNode 3`).
- El bootstrap interno es `ip1:9092,ip2:9092,ip3:9092`.

### Por qué KRaft

- El material S14 del curso ya usa KRaft.
- Kafka 4.x eliminó ZooKeeper; no tiene sentido levantarlo solo para la pauta.
- Menos contenedores y menos RAM que 3 ZooKeeper + 3 brokers.
- La resiliencia evaluable es la misma: 3 brokers, RF 3, ISR, leader election.

**Limitación honesta:** al estar en una sola AZ, se tolera la caída de una
instancia/broker, **no** la caída de la AZ. Para producción: 3 EC2 en 3 AZ o MSK.

### Tópicos

`init-topics.sh` crea de forma idempotente:

| Tópico | Particiones | Réplica | `min.insync.replicas` | Retención |
|---|---:|---:|---:|---|
| `productos360.eventos.auditoria` | 3 | 3 | 2 | 7 días |
| `productos360.eventos.metricas` | 3 | 3 | 2 | 30 días |
| `productos360.eventos.auditoria.dlt` | 3 | 3 | 2 | 30 días |
| `productos360.eventos.metricas.dlt` | 3 | 3 | 2 | 30 días |

### Puertos y seguridad

| Puerto | Origen permitido | Uso |
|---:|---|---|
| 9092 | SG de `producto-service`, SG de `analytics-service` y el propio SG de Kafka | clientes Kafka |
| 9093 | solo el propio SG de Kafka | quórum de controllers |
| 8080 | `admin_cidr` | Kafka UI |
| 22 | `admin_cidr` + EC2 Instance Connect | SSH |

El 9092 **no** se expone a `0.0.0.0/0`. No hay TLS/SASL: el aislamiento es por
VPC + Security Groups. Si la rúbrica pide seguridad adicional, se agrega SCRAM/TLS
sobre esta misma base.

### Prueba de resiliencia

Con el cluster arriba:

```bash
# En kafka1: estado de tópicos, ISR y líderes
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --describe
```

Simular caída de broker (sin tumbar la EC2):

```bash
# En kafka2
docker stop kafka
```

Verificar en Kafka UI / `--describe`:

- ISR de las particiones lideradas por kafka2 baja de 3 a 2.
- Se elige un nuevo líder.
- Productor y consumidor siguen funcionando (`acks=all` + `min.insync.replicas=2`).

Recuperar:

```bash
# En kafka2
docker start kafka
```

Simular caída de host:

```powershell
$ids = terraform -chdir=infra/terraform output -json ec2_kafka_instance_ids | ConvertFrom-Json
aws ec2 stop-instances --instance-ids $ids[1]   # kafka2
# ... demo ...
aws ec2 start-instances --instance-ids $ids[1]
```

Si se detienen 2 de 3 brokers se pierde el quórum de controllers y las escrituras
con `min.insync.replicas=2` fallan. Eso es intencional: protege los datos.

> La IP privada se mantiene al detener/iniciar una EC2, así que el quórum sigue
> válido. Si Terraform **recrea** una instancia, la IP cambia y hay que volver a
> ejecutar `deploy-kafka-cluster.ps1` para regenerar los `.env` de los 3 nodos.

### Operación

```bash
# Logs
docker logs -f kafka

# Estado del contenedor
docker compose -f /opt/kafka/docker-compose.yml ps

# Kafka UI
# http://<ip-publica-kafka3>:8080

# Consola de productor
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic productos360.eventos.auditoria

# Consola de consumidor
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic productos360.eventos.auditoria \
  --from-beginning
```

`docker compose down` conserva el volumen `kafka_data`. **No uses `down -v`**
salvo que quieras borrar todos los datos y volver a formatear el broker.

## Costos aproximados (us-east-1)

| Recurso | Costo mensual encendido |
|---|---:|
| 3 x `t3.small` | ~$45 |
| 3 x 30 GB gp3 | ~$7 |
| 3 IPv4 públicas | ~$11 |
| **Total Kafka** | **~$63/mes** |

Apagando las 3 EC2 fuera de la demo, queda solo el costo de los EBS (~$7/mes).
Las IPs públicas cambian al detener/iniciar; el bootstrap usa IPs privadas, así
que no afecta a Kafka. MSK provisionado con 3 brokers ronda los ~$100/mes y no
se puede apagar; por eso se eligió EC2.

## Troubleshooting

| Síntoma | Causa probable | Solución |
|---|---|---|
| `--replication-factor 3` falla al crear tópicos | no hay 3 brokers arriba todavía | esperar el quórum y re-ejecutar `deploy-kafka-cluster.ps1` |
| Broker no arranca con `InconsistentClusterIdException` | `KAFKA_CLUSTER_ID` cambió | restaurar el ID de `comun.env` o borrar el volumen `kafka_data` |
| `Connection to node -1 could not be established` | advertised listener con IP incorrecta | revisar `BROKER_PRIVATE_IP` en `/opt/kafka/.env` y re-desplegar |
| Kafka UI no abre | puerto 8080 restringido a `admin_cidr` | actualizar `admin_cidr` en `terraform.tfvars` y re-aplicar |
| `docker compose` no existe en la EC2 | user_data aún no terminó | esperar 1-2 minutos y reintentar |
| Los tópicos tienen 1 partición | se crearon antes con `auto.create` | borrar el tópico y volver a ejecutar `init-topics.sh` |

## Documentación relacionada

- [`terraform/README.md`](terraform/README.md): recursos, variables y outputs.
- [`envs/README.md`](envs/README.md): secretos y `.env` por servicio.
- [`scripts/README.md`](scripts/README.md): guía de scripts de despliegue.
- [`docker/README.md`](docker/README.md): stack local y cluster Kafka.
- [`docker/kafka-cluster/README.md`](docker/kafka-cluster/README.md): detalle del cluster.

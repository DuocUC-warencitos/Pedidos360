# Cluster Kafka KRaft (3 brokers)

Compose para desplegar **un broker Kafka por EC2**, con 3 EC2 `t3.small` en la
misma subnet/AZ. Usa Kafka 4.2.2 en modo KRaft combinado (broker + controller),
sin ZooKeeper.

## Archivos

| Archivo | Función |
|---|---|
| `docker-compose.yml` | broker parametrizado por `.env` + Kafka UI opcional |
| `init-topics.sh` | crea los 4 tópicos con 3 particiones, RF 3 y min ISR 2 |

El `.env` de cada nodo **no se versiona**; lo genera
`infra/scripts/deploy-kafka-cluster.ps1` a partir de los outputs de Terraform.

## Variables del `.env` por nodo

```env
KAFKA_NODE_ID=1
BROKER_PRIVATE_IP=10.0.0.11
QUORUM_VOTERS=1@10.0.0.11:9093,2@10.0.0.12:9093,3@10.0.0.13:9093
KAFKA_CLUSTER_ID=abcdefghijklmnopqrstuv
BOOTSTRAP_SERVERS=10.0.0.11:9092,10.0.0.12:9092,10.0.0.13:9092
KAFKA_HEAP_OPTS=-Xms512m -Xmx1g
UI_PORT=8080
```

`KAFKA_CLUSTER_ID` se genera una sola vez y se guarda en `infra/envs/comun.env`.
No lo cambies después del primer arranque: los brokers no levantan con un ID
distinto al de sus datos.

## Despliegue

```powershell
# Requiere terraform apply previo
.\infra\scripts\deploy-kafka-cluster.ps1

# Kafka UI en kafka2 en vez de kafka3
.\infra\scripts\deploy-kafka-cluster.ps1 -UiNode 2

# Solo levantar brokers, sin UI ni tópicos
.\infra\scripts\deploy-kafka-cluster.ps1 -SkipUi -SkipTopicInit
```

## Puertos

| Puerto | Uso |
|---:|---|
| 9092 | clientes Kafka (producto-service, analytics-service) |
| 9093 | quórum de controllers (solo entre brokers) |
| 8080 | Kafka UI (solo `admin_cidr`) |

El 9092 no se publica a Internet: el SG solo permite el SG de producto, el SG de
analytics y el propio SG de Kafka.

## Configuración clave

```yaml
KAFKA_PROCESS_ROLES: broker,controller
KAFKA_CONTROLLER_QUORUM_VOTERS: 1@ip1:9093,2@ip2:9093,3@ip3:9093
KAFKA_DEFAULT_REPLICATION_FACTOR: 3
KAFKA_MIN_INSYNC_REPLICAS: 2
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 3
KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 2
KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"
```

Con esto:

- Cada tópico tiene 3 réplicas.
- Se tolera la caída de 1 broker sin perder escrituras confirmadas.
- Si se caen 2 de 3, se pierde el quórum y las escrituras con `acks=all` +
  `min.insync.replicas=2` fallan en vez de aceptar datos sin replicar.

## Tópicos

`init-topics.sh` crea:

- `productos360.eventos.auditoria` (7 días)
- `productos360.eventos.metricas` (30 días)
- `productos360.eventos.auditoria.dlt` (30 días)
- `productos360.eventos.metricas.dlt` (30 días)

Todos con 3 particiones, RF 3 y `min.insync.replicas=2`. Es idempotente:
`--create --if-not-exists` + `kafka-configs --alter` para asegurar la retención.

## Comandos útiles

```bash
# Estado del cluster
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --describe

# Consumir offsets de un grupo
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group productos360-metrics-group

# Logs
docker logs -f kafka

# Kafka UI (desde tu IP admin)
# http://<ip-publica-del-nodo-ui>:8080
```

## Resiliencia

```bash
# En kafka2
docker stop kafka

# Ver ISR/leader en kafka1
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --describe

# Recuperar
docker start kafka
```

Esto demuestra failover de broker. Para demostrar caída de host, detén la EC2
completa:

```powershell
$ids = terraform -chdir=infra/terraform output -json ec2_kafka_instance_ids | ConvertFrom-Json
aws ec2 stop-instances --instance-ids $ids[1]
aws ec2 start-instances --instance-ids $ids[1]
```

## Limitaciones

- Las 3 EC2 están en la misma AZ: no hay tolerancia a falla de AZ.
- Sin TLS/SASL: el aislamiento es VPC + Security Groups.
- El volumen `kafka_data` vive en el EBS raíz de cada EC2. Si se termina la
  instancia, se pierde el broker; los tópicos se pueden recrear con
  `init-topics.sh`, pero los datos históricos no.
- `docker compose down` conserva el volumen. `docker compose down -v` lo borra.

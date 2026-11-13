# infra/docker

Composes Docker de Pedidos360.

## Stack local (`docker-compose.yml`)

Levanta el entorno completo en un solo host para desarrollo:

- `pedidos-angular-app`, `pedidos-gateway`
- `pedidos-service`, `producto-service`, `rabbit-admin-service`, `analytics-service`
- `rabbitmq` (1 nodo)
- `kafka` (1 broker KRaft, **solo dev**)
- `kafka-ui` en `http://localhost:8081`

```bash
docker compose -f infra/docker/docker-compose.yml --env-file infra/docker/.env up -d
```

> El Kafka local es de **1 broker con RF=1**. Sirve para desarrollo, pero no
> permite demostrar replicación ni failover. Para eso está `kafka-cluster/`.

## Cluster Kafka (`kafka-cluster/`)

Compose que se despliega **un broker por EC2** (3 EC2 en total) para el entorno
cloud. No se levanta con el stack local completo.

- [`kafka-cluster/docker-compose.yml`](kafka-cluster/docker-compose.yml)
- [`kafka-cluster/init-topics.sh`](kafka-cluster/init-topics.sh)
- [`kafka-cluster/README.md`](kafka-cluster/README.md)

Se despliega con:

```powershell
.\infra\scripts\deploy-kafka-cluster.ps1
```

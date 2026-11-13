## Git

```bash
git config --global user.email "you@example.com"
git config --global user.name "Your Name"

git add .
git commit -m "tu mensaje"
git push
```

# Pedidos360

Sistema de pedidos y control de stock con microservicios Spring Boot, frontend
Angular y mensajería con RabbitMQ + Kafka.

## Arquitectura

| Componente | Tecnología | Rol |
|---|---|---|
| `frontend` | Angular | UI de pedidos, productos y administración |
| `pedidos-gateway` | Spring Cloud Gateway | Gateway local (en AWS se usa API Gateway) |
| `pedidos-service` | Spring Boot + PostgreSQL | Pedidos, reservas y notificaciones |
| `producto-service` | Spring Boot + PostgreSQL | Catálogo, stock y eventos de inventario |
| `rabbitmq_admin_service` | Spring Boot + PostgreSQL | Notificaciones, auditoría y métricas de Rabbit |
| `analytics-service` | Spring Boot + Kafka Streams | KPIs de inventario y alertas |
| RabbitMQ | RabbitMQ 3 | Eventos de pedidos/notificaciones |
| Kafka | Kafka 4.2.2 KRaft | Eventos de productos e inventario |

En AWS:

- API Gateway con JWT de Entra ID.
- 2 RDS PostgreSQL (`pedidos` y `productos`).
- EC2 para los servicios, RabbitMQ y analytics.
- Cluster Kafka de **3 brokers `t3.small`** en KRaft, con 3 particiones y
  réplica 3 por tópico (misma subnet/AZ en el laboratorio).

## Estructura del repositorio

```
Pedidos360/
├── backend/
│   ├── pedidos-service/
│   ├── producto-service/
│   ├── analytics-service/
│   ├── pedidos-gateway/
│   └── rabbitmq_admin_service/
├── frontend/                 # Angular
├── infra/
│   ├── terraform/            # AWS: RDS, EC2, SGs, API Gateway, Kafka
│   ├── docker/               # compose local + cluster Kafka cloud
│   ├── envs/                 # .env.example y secretos locales
│   └── scripts/              # deploy-all.ps1 y scripts por servicio
├── docs/                     # plan funcional y rúbricas
├── README.md
└── COMANDOS_LAB.txt
```

## 1. Ejecutar

```bash
npm install
npm start
```

El frontend está en `frontend/`:

```bash
cd frontend
npm install
npm start
```

## 2. Stack local con Docker

```bash
docker compose -f infra/docker/docker-compose.yml --env-file infra/docker/.env up -d
```

Servicios locales:

- Frontend: `http://localhost`
- RabbitMQ Management: `http://localhost:15672`
- Kafka UI: `http://localhost:8081`

> El compose local usa **1 broker Kafka** para desarrollo. El cluster de 3
> brokers (KRaft, RF 3) está en `infra/docker/kafka-cluster/` y se despliega en
> AWS con `deploy-kafka-cluster.ps1`.

## 3. Despliegue en AWS

Requisitos: Terraform, AWS Learner Lab, Docker Desktop, PowerShell y la key
`vockey`/`.pem`.

```powershell
# 1. Secretos compartidos
Copy-Item infra\envs\comun.env.example infra\envs\comun.env
notepad infra\envs\comun.env

# 2. Variables de Terraform
Copy-Item infra\terraform\terraform.tfvars.example infra\terraform\terraform.tfvars
notepad infra\terraform\terraform.tfvars

# 3. Todo: Terraform + Kafka + Rabbit + servicios
.\infra\scripts\deploy-all.ps1 -AutoApprove
```

Solo Kafka (si Terraform ya está aplicado):

```powershell
.\infra\scripts\deploy-kafka-cluster.ps1
```

Sin Kafka:

```powershell
.\infra\scripts\deploy-all.ps1 -SkipKafka
```

## 4. Kafka (KRaft)

- 3 EC2 `t3.small`, una por broker, misma subnet/AZ.
- Kafka 4.2.2 en modo combinado (broker + controller), sin ZooKeeper.
- 3 particiones y réplica 3 por tópico.
- `min.insync.replicas=2` y productores con `acks=all`.
- Kafka UI en el broker 3 (solo accesible desde `admin_cidr`).

Detalle completo:

- [`infra/README.md`](infra/README.md)
- [`infra/docker/kafka-cluster/README.md`](infra/docker/kafka-cluster/README.md)
- [`infra/terraform/README.md`](infra/terraform/README.md)
- [`infra/scripts/README.md`](infra/scripts/README.md)

## 5. Prueba rápida de resiliencia

```bash
# En kafka1: ver líderes, réplicas e ISR
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --describe

# En kafka2: simular caída de broker
docker stop kafka

# Verificar ISR 3 -> 2 y que producer/consumer siguen
# Recuperar
docker start kafka
```

Se tolera la caída de 1 broker. Si se caen 2 de 3, se pierde el quórum y las
escrituras con `min.insync.replicas=2` fallan para no aceptar datos sin replicar.

## Notas

- La pauta EP6 menciona ZooKeeper; el docente autorizó usar **KRaft**. Este
  repositorio implementa KRaft.
- El plan funcional en `docs/` puede mencionar ZooKeeper; la infraestructura
  implementada está documentada en `infra/`.
- Los secretos (`infra/envs/comun.env`, `infra/envs/aws.env`, `.env`) están
  gitignored. Los `.env.example` sí se versionan.

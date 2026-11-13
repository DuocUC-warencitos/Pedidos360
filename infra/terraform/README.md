# Infra Terraform — Pedidos360

## Estructura

```
infra/terraform/
├── provider.tf              # AWS provider + default_tags
├── variables.tf             # VPC, EC2, RDS, JWT, GHCR y Kafka
├── data.tf                  # VPC default, subnets públicas, AMI AL2023
├── security.tf              # SG pedidos, producto y RDS
├── security_rabbit.tf       # SG cluster RabbitMQ y rabbit-admin
├── security_kafka.tf        # SG brokers Kafka + Kafka UI
├── security_analiticas.tf   # SG de analytics-service
├── rds.tf                   # 2 RDS PostgreSQL 15
├── ec2.tf                   # EC2 pedidos + producto
├── ec2_rabbit.tf            # EC2 RabbitMQ + rabbit-admin
├── ec2_kafka.tf             # 3 EC2 t3.small para Kafka KRaft
├── ec2_analiticas.tf        # EC2 t3.small para analytics-service
├── apigw.tf                 # HTTP API + JWT Authorizer Entra ID
├── outputs.tf               # IPs, JDBC, API Gateway y Kafka
├── user_data.sh.tmpl        # bootstrap de servicios Spring
├── user_data_docker.sh.tmpl # bootstrap Docker + compose (Rabbit/Kafka)
├── terraform.tfvars.example -> copiar a terraform.tfvars (gitignored)
└── README.md
```

## Uso lab (VPC default)

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
nano terraform.tfvars  # completa db_password (32 chars), key_name=vockey

# Credenciales lab Learner Lab
export AWS_ACCESS_KEY_ID=... AWS_SECRET_ACCESS_KEY=... AWS_SESSION_TOKEN=...

terraform init
terraform fmt -check
terraform validate
terraform plan -out=tfplan
terraform apply tfplan
terraform output apigw_url  # -> NG_APP_API_GATEWAY_URL para Vercel
terraform output rds_pedidos_endpoint

# En EC2, el .env se crea automático vía user_data.sh
# Local prueba prod (fuente única ../../.env.example):
cp ../../.env.example ../../.env
nano ../../.env
docker compose -f ../../docker-compose.prod.yml --env-file ../../.env pull && up -d
```

## Kafka (3 brokers KRaft)

`ec2_kafka.tf` crea **3 EC2 `t3.small`** en la misma subnet/AZ
(`local.public_subnet_ids[0]`), cada una con:

- 30 GB gp3.
- Docker instalado por `user_data_docker.sh.tmpl` en `/opt/kafka`.
- Un broker Kafka 4.2.2 en modo KRaft combinado (broker + controller).
- Security Group `ec2_kafka` con:
  - 9092 desde `producto-service`, `analytics-service` y el propio SG de Kafka.
  - 9093 (controllers) solo desde el propio SG de Kafka.
  - 8080 (Kafka UI) desde `admin_cidr`.
  - 22 desde `admin_cidr` + EC2 Instance Connect.

Terraform **no** crea tópicos ni levanta los contenedores. Eso lo hace
`infra/scripts/deploy-kafka-cluster.ps1`, que:

1. Genera el `.env` de cada broker con `KAFKA_NODE_ID`, IP privada y quórum.
2. Sube `infra/docker/kafka-cluster/` y levanta el broker.
3. Crea los 6 tópicos con 3 particiones, RF 3 y `min.insync.replicas=2`.

Variables nuevas:

```hcl
kafka_instance_type = "t3.small"
kafka_volume_size   = 30
```

Outputs nuevos:

```bash
terraform output kafka_bootstrap_servers   # ip1:9092,ip2:9092,ip3:9092
terraform output ec2_kafka_private_ips
terraform output ec2_kafka_public_ips
terraform output ec2_kafka_instance_ids    # para pruebas de caída
terraform output kafka_ui_url              # http://ip-kafka3:8080
```

> Las 3 EC2 están en la misma AZ: toleran la caída de una instancia/broker, no
> la de la AZ. En producción se distribuyen en 3 AZ o se usa Amazon MSK.

## Analytics (Kafka Streams)

`ec2_analiticas.tf` crea **1 EC2 `t3.small`** (20 GB gp3) con el SG
`ec2_analiticas`, Docker instalado por `user_data_docker.sh.tmpl` y el directorio
`/opt/analytics-service`. Consume `productos360.eventos.productos` y
`productos360.eventos.stock` desde el cluster Kafka (puerto 9092 permitido por
`security_kafka.tf`).

Variables:

```hcl
analiticas_instance_type = "t3.small"
analiticas_volume_size   = 20
```

Outputs:

```bash
terraform output ec2_analiticas_public_ip
terraform output ec2_analiticas_private_ip
```

`apigw.tf` expone `ANY /api/v1/admin/analiticas` y
`ANY /api/v1/admin/analiticas/{proxy+}` con el authorizer JWT; los `OPTIONS`
quedan sin autorizador para el preflight CORS. El servicio se despliega con
`infra/scripts/deploy-analytics-service.ps1`.

## Notas EP1

- RDS solo desde su EC2 (`sg-rds-*` ingress `sg-ec2-*`).
- EC2 `8080` desde `0.0.0.0/0` (API Gateway público lab); con VPC Link restringir a `sg_apigw`.
- API Gateway JWT `issuer https://login.microsoftonline.com/1f3a.../v2.0` + audience `api://374ba...`.
- GHCR públicas `ghcr.io/duocuc-warencitos/*` sin login en EC2 si son públicas; si son privadas, define GHCR_USERNAME/GHCR_TOKEN en `infra\envs\comun.env` (los deploy scripts hacen docker login en la EC2).

## Notas Kafka

- `KAFKA_CLUSTER_ID` se guarda en `infra/envs/comun.env` (gitignored) y debe ser
  el mismo en los 3 brokers. Si cambia, los brokers no levantan con datos viejos.
- El bootstrap interno usa IPs privadas. No necesitas Elastic IP para Kafka; las
  IPs públicas solo se usan para SSH y Kafka UI.
- Al detener/iniciar una EC2 la IP privada se mantiene. Si Terraform recrea la
  instancia, vuelve a ejecutar `deploy-kafka-cluster.ps1`.

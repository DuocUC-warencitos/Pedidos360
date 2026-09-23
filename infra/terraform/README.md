# Infra Terraform — Pedidos360 EP1

## Estructura
```
infra/terraform/
├── provider.tf, variables.tf, data.tf, outputs.tf
├── security.tf (4 SG: ec2-pedidos/producto, rds-pedidos/producto)
├── rds.tf (2 RDS postgres 15)
├── ec2.tf (2 EC2 t3.micro + user_data)
├── apigw.tf (HTTP API + JWT Authorizer Entra ID)
├── user_data.sh.tmpl
├── terraform.tfvars.example  -> copiar a terraform.tfvars (gitignored)
└── (usa ../../.env.example — fuente única) -> .env EC2 generado vía user_data
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
terraform output apigw_url  # → NG_APP_API_GATEWAY_URL para Vercel
terraform output rds_pedidos_endpoint

# En EC2, el .env se crea automático vía user_data.sh
# Local prueba prod (fuente única ../../.env.example):
cp ../../.env.example ../../.env
nano ../../.env
docker compose -f ../../docker-compose.prod.yml --env-file ../../.env pull && up -d
```

## Notas EP1
- RDS solo desde su EC2 (`sg-rds-*` ingress `sg-ec2-*`).
- EC2 `8080` desde `0.0.0.0/0` (API Gateway público lab); con VPC Link restringir a `sg_apigw`.
- API Gateway JWT `issuer https://login.microsoftonline.com/1f3a.../v2.0` + audience `api://374ba...`.
- GHCR públicas `ghcr.io/duocuc-warencitos/*` sin login en EC2.

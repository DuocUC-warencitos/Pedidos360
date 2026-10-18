# Infra Terraform â€” Pedidos360 EP1

## Estructura
```
infra/terraform/
â”œâ”€â”€ provider.tf, variables.tf, data.tf, outputs.tf
â”œâ”€â”€ security.tf (4 SG: ec2-pedidos/producto, rds-pedidos/producto)
â”œâ”€â”€ rds.tf (2 RDS postgres 15)
â”œâ”€â”€ ec2.tf (2 EC2 t3.micro + user_data)
â”œâ”€â”€ apigw.tf (HTTP API + JWT Authorizer Entra ID)
â”œâ”€â”€ user_data.sh.tmpl
â”œâ”€â”€ terraform.tfvars.example  -> copiar a terraform.tfvars (gitignored)
â””â”€â”€ (usa ../../.env.example â€” fuente Ãºnica) -> .env EC2 generado vÃ­a user_data
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
terraform output apigw_url  # â†’ NG_APP_API_GATEWAY_URL para Vercel
terraform output rds_pedidos_endpoint

# En EC2, el .env se crea automÃ¡tico vÃ­a user_data.sh
# Local prueba prod (fuente Ãºnica ../../.env.example):
cp ../../.env.example ../../.env
nano ../../.env
docker compose -f ../../docker-compose.prod.yml --env-file ../../.env pull && up -d
```

## Notas EP1
- RDS solo desde su EC2 (`sg-rds-*` ingress `sg-ec2-*`).
- EC2 `8080` desde `0.0.0.0/0` (API Gateway pÃºblico lab); con VPC Link restringir a `sg_apigw`.
- API Gateway JWT `issuer https://login.microsoftonline.com/1f3a.../v2.0` + audience `api://374ba...`.
- GHCR pÃºblicas `ghcr.io/duocuc-warencitos/*` sin login en EC2 si son pÃºblicas; si son privadas, define GHCR_USERNAME/GHCR_TOKEN en infra\envs\comun.env (los deploy scripts hacen docker login en la EC2).

variable "aws_region" {
  description = "Región AWS del laboratorio"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Entorno (dev/prod/lab)"
  type        = string
  default     = "prod"
}

variable "project" {
  description = "Nombre proyecto"
  type        = string
  default     = "pedidos360"
}

# ---------- VPC ----------
variable "vpc_id" {
  description = "VPC default del laboratorio (vacío = auto-detecta default VPC)"
  type        = string
  default     = ""
}

# ---------- EC2 ----------
variable "key_name" {
  description = "Key pair existente en la región (vockey en Learner Lab)"
  type        = string
  default     = "vockey"
}

variable "instance_type" {
  description = "Tipo EC2"
  type        = string
  default     = "t3.micro"
}

variable "ami_id" {
  description = "AMI Amazon Linux 2023 (vacío = último AL2023 x86_64)"
  type        = string
  default     = ""
}

# ---------- RDS ----------
variable "rds_instance_class" {
  description = "Clase RDS"
  type        = string
  default     = "db.t3.micro"
}

variable "rds_allocated_storage" {
  description = "Storage RDS GB"
  type        = number
  default     = 20
}

variable "db_name_pedidos" {
  type    = string
  default = "pedidos"
}

variable "db_name_producto" {
  type    = string
  default = "productos"
}

variable "db_username_pedidos" {
  type    = string
  default = "pedidos_user"
}

variable "db_username_producto" {
  type    = string
  default = "producto_user"
}

variable "db_password" {
  description = "Password maestro RDS (32+ chars, mismo para ambos o distinto si prefieres)"
  type        = string
  sensitive   = true
}

variable "db_password_producto" {
  description = "Password RDS producto (vacío = usa db_password)"
  type        = string
  sensitive   = true
  default     = ""
}

# ---------- JWT / API Gateway ----------
variable "jwt_issuer_uri" {
  description = "Entra ID issuer URI (https://login.microsoftonline.com/{tenant}/v2.0)"
  type        = string
  default     = "https://login.microsoftonline.com/1f3a849e-c198-4ff7-b67c-d17f15cbc152/v2.0"
}

variable "jwt_audience" {
  description = "Audience del Access Token (api://...)"
  type        = string
  default     = "api://374ba786-74ed-4b20-a6f5-b115c2e58625/Pedidos.Read"
}

variable "cors_origins" {
  description = "CORS allowed origins para API Gateway y Spring"
  type        = string
  default     = "http://localhost:4200,https://pedidos360.vercel.app"
}

# ---------- GHCR / App ----------
variable "ghcr_image_pedidos" {
  type    = string
  default = "ghcr.io/neytan2214/pedidos-service:latest"
}

variable "ghcr_image_producto" {
  type    = string
  default = "ghcr.io/neytan2214/producto-service:latest"
}

variable "tag" {
  type    = string
  default = "latest"
}

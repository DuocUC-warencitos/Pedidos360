data "aws_vpc" "default" {
  default = var.vpc_id == "" ? true : false
  id      = var.vpc_id != "" ? var.vpc_id : null
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

data "aws_subnets" "public" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
  filter {
    name   = "map-public-ip-on-launch"
    values = ["true"]
  }
}

data "aws_ami" "al2023" {
  count       = var.ami_id == "" ? 1 : 0
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

locals {
  ami_id           = var.ami_id != "" ? var.ami_id : data.aws_ami.al2023[0].id
  public_subnet_ids = length(data.aws_subnets.public.ids) > 0 ? data.aws_subnets.public.ids : data.aws_subnets.default.ids
  db_password_producto_eff = var.db_password_producto != "" ? var.db_password_producto : var.db_password
}

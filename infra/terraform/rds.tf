resource "aws_db_subnet_group" "main" {
  name       = "${var.project}-db-subnet"
  subnet_ids = data.aws_subnets.default.ids
  tags       = { Name = "${var.project}-db-subnet" }
}

resource "aws_db_instance" "pedidos" {
  identifier              = "${var.project}-pedidos"
  engine                  = "postgres"
  engine_version          = "15"
  instance_class          = var.rds_instance_class
  allocated_storage       = var.rds_allocated_storage
  db_name                 = var.db_name_pedidos
  username                = var.db_username_pedidos
  password                = var.db_password
  db_subnet_group_name    = aws_db_subnet_group.main.name
  vpc_security_group_ids  = [aws_security_group.rds_pedidos.id]
  publicly_accessible     = false
  skip_final_snapshot     = true
  deletion_protection     = false
  apply_immediately       = true
  backup_retention_period = 0
  tags                    = { Name = "${var.project}-pedidos" }
}

resource "aws_db_instance" "producto" {
  identifier              = "${var.project}-producto"
  engine                  = "postgres"
  engine_version          = "15"
  instance_class          = var.rds_instance_class
  allocated_storage       = var.rds_allocated_storage
  db_name                 = var.db_name_producto
  username                = var.db_username_producto
  password                = local.db_password_producto_eff
  db_subnet_group_name    = aws_db_subnet_group.main.name
  vpc_security_group_ids  = [aws_security_group.rds_producto.id]
  publicly_accessible     = false
  skip_final_snapshot     = true
  deletion_protection     = false
  apply_immediately       = true
  backup_retention_period = 0
  tags                    = { Name = "${var.project}-producto" }
}

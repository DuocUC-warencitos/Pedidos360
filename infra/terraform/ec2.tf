locals {
  pedidos_spring_url  = "jdbc:postgresql://${aws_db_instance.pedidos.address}:5432/${var.db_name_pedidos}?ssl=true&sslmode=require"
  producto_spring_url = "jdbc:postgresql://${aws_db_instance.producto.address}:5432/${var.db_name_producto}?ssl=true&sslmode=require"
}

resource "aws_instance" "pedidos" {
  ami                         = local.ami_id
  instance_type               = var.instance_type
  subnet_id                   = local.public_subnet_ids[0]
  vpc_security_group_ids      = [aws_security_group.ec2_pedidos.id]
  key_name                    = var.key_name
  associate_public_ip_address = true

  user_data = templatefile("${path.module}/user_data.sh.tmpl", {
    spring_datasource_url = local.pedidos_spring_url
    db_username           = var.db_username_pedidos
    db_password           = var.db_password
    jwt_issuer_uri        = var.jwt_issuer_uri
    jwt_audience          = var.jwt_audience
    cors_origins          = var.cors_origins
    tag                   = var.tag
    producto_service_url  = "http://${aws_instance.producto.private_ip}:8080"
    ghcr_image            = var.ghcr_image_pedidos
  })

  tags = { Name = "${var.project}-pedidos-ec2" }

  depends_on = [aws_db_instance.pedidos]
}

resource "aws_instance" "producto" {
  ami                         = local.ami_id
  instance_type               = var.instance_type
  subnet_id                   = try(local.public_subnet_ids[1], local.public_subnet_ids[0])
  vpc_security_group_ids      = [aws_security_group.ec2_producto.id]
  key_name                    = var.key_name
  associate_public_ip_address = true

  user_data = templatefile("${path.module}/user_data.sh.tmpl", {
    spring_datasource_url = local.producto_spring_url
    db_username           = var.db_username_producto
    db_password           = local.db_password_producto_eff
    jwt_issuer_uri        = var.jwt_issuer_uri
    jwt_audience          = var.jwt_audience
    cors_origins          = var.cors_origins
    tag                   = var.tag
    producto_service_url  = ""
    ghcr_image            = var.ghcr_image_producto
  })

  tags = { Name = "${var.project}-producto-ec2" }

  depends_on = [aws_db_instance.producto]
}

resource "aws_instance" "frontend" {
  ami                         = local.ami_id
  instance_type               = var.frontend_instance_type
  subnet_id                   = try(local.public_subnet_ids[2], local.public_subnet_ids[0])
  vpc_security_group_ids      = [aws_security_group.ec2_frontend.id]
  key_name                    = var.key_name
  associate_public_ip_address = true

  user_data = templatefile("${path.module}/user_data_frontend.sh.tmpl", {
    ghcr_image_frontend = var.ghcr_image_frontend
  })

  tags = { Name = "${var.project}-frontend-ec2" }
}

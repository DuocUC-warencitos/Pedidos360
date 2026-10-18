# ---------- EC2 clúster RabbitMQ (rabbit1 + rabbit2 vía Docker Compose) ----------
resource "aws_instance" "rabbit" {
  ami                         = local.ami_id
  instance_type               = var.instance_type
  subnet_id                   = local.public_subnet_ids[0]
  vpc_security_group_ids      = [aws_security_group.ec2_rabbit.id]
  key_name                    = var.key_name
  associate_public_ip_address = true

  user_data = templatefile("${path.module}/user_data_docker.sh.tmpl", {
    app_dir = "/opt/rabbit-cluster"
  })

  tags = { Name = "${var.project}-rabbit-ec2" }
}

# ---------- EC2 rabbit-admin-service ----------
resource "aws_instance" "rabbit_admin" {
  ami                         = local.ami_id
  instance_type               = var.instance_type
  subnet_id                   = try(local.public_subnet_ids[1], local.public_subnet_ids[0])
  vpc_security_group_ids      = [aws_security_group.ec2_rabbit_admin.id]
  key_name                    = var.key_name
  associate_public_ip_address = true

  user_data = templatefile("${path.module}/user_data_docker.sh.tmpl", {
    app_dir = "/opt/rabbit-admin"
  })

  tags = { Name = "${var.project}-rabbit-admin-ec2" }
}

# ---------- EC2 analytics-service (Kafka Streams) ----------
# Consume los topicos de productos/stock desde el cluster Kafka y expone
# /api/v1/admin/analiticas/* detras del API Gateway.
resource "aws_instance" "analiticas" {
  ami                         = local.ami_id
  instance_type               = var.analiticas_instance_type
  subnet_id                   = local.public_subnet_ids[0]
  vpc_security_group_ids      = [aws_security_group.ec2_analiticas.id]
  key_name                    = var.key_name
  associate_public_ip_address = true

  root_block_device {
    volume_size = var.analiticas_volume_size
    volume_type = "gp3"
  }

  user_data = templatefile("${path.module}/user_data_docker.sh.tmpl", {
    app_dir = "/opt/analytics-service"
  })

  tags = { Name = "${var.project}-analiticas-ec2" }
}

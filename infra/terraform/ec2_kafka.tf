# ---------- EC2 cluster Kafka (3 brokers KRaft en la misma subnet/AZ) ----------
# Los 3 brokers comparten subnet y AZ para simplificar el laboratorio.
# Tolera la caida de una instancia/broker; NO tolera la caida de la AZ.
# En produccion: una EC2 por AZ o Amazon MSK.
resource "aws_instance" "kafka" {
  count = 3

  ami                         = local.ami_id
  instance_type               = var.kafka_instance_type
  subnet_id                   = local.public_subnet_ids[0]
  vpc_security_group_ids      = [aws_security_group.ec2_kafka.id]
  key_name                    = var.key_name
  associate_public_ip_address = true

  root_block_device {
    volume_size = var.kafka_volume_size
    volume_type = "gp3"
  }

  user_data = templatefile("${path.module}/user_data_docker.sh.tmpl", {
    app_dir = "/opt/kafka"
  })

  tags = { Name = "${var.project}-kafka-${count.index + 1}" }
}

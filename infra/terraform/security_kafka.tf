# ---------- SG EC2 Kafka (3 brokers KRaft + Kafka UI) ----------
resource "aws_security_group" "ec2_kafka" {
  name_prefix = "${var.project}-ec2-kafka-"
  description = "EC2 Kafka: brokers KRaft + UI admin; 9092 solo desde apps, nunca publico"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "Kafka brokers entre si"
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    self        = true
  }

  ingress {
    description = "Controller quorum KRaft"
    from_port   = 9093
    to_port     = 9093
    protocol    = "tcp"
    self        = true
  }

  ingress {
    description     = "Kafka desde producto-service"
    from_port       = 9092
    to_port         = 9092
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2_producto.id]
  }

  ingress {
    description     = "Kafka desde analytics-service"
    from_port       = 9092
    to_port         = 9092
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2_analiticas.id]
  }

  ingress {
    description = "SSH solo IP del admin"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.admin_cidr]
  }

  ingress {
    description = "SSH desde EC2 Instance Connect (consola AWS)"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.instance_connect_cidr]
  }

  ingress {
    description = "Kafka UI solo IP del admin"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = [var.admin_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project}-ec2-kafka" }
}

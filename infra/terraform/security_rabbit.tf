# ---------- SG EC2 RabbitMQ (clúster 2 nodos) ----------
resource "aws_security_group" "ec2_rabbit" {
  name_prefix = "${var.project}-ec2-rabbit-"
  description = "EC2 RabbitMQ: SSH + AMQP desde apps + Management UI"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH lab"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description     = "AMQP desde rabbit-admin"
    from_port       = 5672
    to_port         = 5672
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2_rabbit_admin.id]
  }

  ingress {
    description     = "AMQP desde pedidos-service"
    from_port       = 5672
    to_port         = 5672
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2_pedidos.id]
  }

  ingress {
    description = "Management UI (demo/video)"
    from_port   = 15672
    to_port     = 15672
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project}-ec2-rabbit" }
}

# ---------- SG EC2 rabbit-admin-service ----------
resource "aws_security_group" "ec2_rabbit_admin" {
  name_prefix = "${var.project}-ec2-rabbit-admin-"
  description = "EC2 rabbit-admin-service: SSH + 8080 desde API Gateway"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH lab"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "App 8080 desde API Gateway publico"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project}-ec2-rabbit-admin" }
}

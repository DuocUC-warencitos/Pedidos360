# ---------- SG EC2 analytics-service ----------
# Se crea aunque la EC2 se agregue despues: el SG de Kafka ya lo referencia
# para permitir el consumo de los topicos desde analytics-service.
resource "aws_security_group" "ec2_analiticas" {
  name_prefix = "${var.project}-ec2-analiticas-"
  description = "EC2 analytics-service: SSH admin + 8080 desde API Gateway"
  vpc_id      = data.aws_vpc.default.id

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

  tags = { Name = "${var.project}-ec2-analiticas" }
}

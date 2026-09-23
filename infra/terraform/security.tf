# ---------- SG EC2 Pedidos ----------
resource "aws_security_group" "ec2_pedidos" {
  name_prefix = "${var.project}-ec2-pedidos-"
  description = "EC2 pedidos-service: SSH + 8080 desde API Gateway / lab"
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

  tags = { Name = "${var.project}-ec2-pedidos" }
}

# ---------- SG EC2 Producto ----------
resource "aws_security_group" "ec2_producto" {
  name_prefix = "${var.project}-ec2-producto-"
  description = "EC2 producto-service: SSH + 8080 + Feign desde pedidos"
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

  ingress {
    description     = "Feign pedidos a producto"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2_pedidos.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project}-ec2-producto" }
}

# ---------- SG RDS Pedidos (solo desde su EC2) ----------
resource "aws_security_group" "rds_pedidos" {
  name_prefix = "${var.project}-rds-pedidos-"
  description = "RDS pedidos solo desde ec2-pedidos"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "Postgres desde ec2-pedidos"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2_pedidos.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project}-rds-pedidos" }
}

# ---------- SG RDS Producto (solo desde su EC2) ----------
resource "aws_security_group" "rds_producto" {
  name_prefix = "${var.project}-rds-producto-"
  description = "RDS producto solo desde ec2-producto"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "Postgres desde ec2-producto"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2_producto.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project}-rds-producto" }
}

# ---------- SG EC2 Frontend (80 publico) ----------
resource "aws_security_group" "ec2_frontend" {
  name_prefix = "${var.project}-ec2-frontend-"
  description = "EC2 frontend: SSH + 80 HTTP"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH lab"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP 80"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project}-ec2-frontend" }
}

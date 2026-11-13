output "vpc_id" {
  value = data.aws_vpc.default.id
}

output "public_subnet_ids" {
  value = local.public_subnet_ids
}

output "rds_pedidos_endpoint" {
  value = aws_db_instance.pedidos.endpoint
}

output "rds_producto_endpoint" {
  value = aws_db_instance.producto.endpoint
}

output "rds_pedidos_address" {
  value = aws_db_instance.pedidos.address
}

output "rds_producto_address" {
  value = aws_db_instance.producto.address
}

output "ec2_pedidos_public_ip" {
  value = aws_instance.pedidos.public_ip
}

output "ec2_producto_public_ip" {
  value = aws_instance.producto.public_ip
}

output "ec2_pedidos_private_ip" {
  value = aws_instance.pedidos.private_ip
}

output "ec2_producto_private_ip" {
  value = aws_instance.producto.private_ip
}

output "ec2_rabbit_public_ip" {
  value = aws_instance.rabbit.public_ip
}

output "ec2_rabbit_private_ip" {
  value = aws_instance.rabbit.private_ip
}

output "ec2_rabbit_admin_public_ip" {
  value = aws_instance.rabbit_admin.public_ip
}

output "ec2_rabbit_admin_private_ip" {
  value = aws_instance.rabbit_admin.private_ip
}

output "ec2_analiticas_public_ip" {
  description = "IP publica de analytics-service (SSH y deploy)"
  value       = aws_instance.analiticas.public_ip
}

output "ec2_analiticas_private_ip" {
  value = aws_instance.analiticas.private_ip
}

output "apigw_url" {
  description = "URL base del API Gateway (para NG_APP_API_GATEWAY_URL en Vercel)"
  value       = aws_apigatewayv2_stage.prod.invoke_url
}

output "apigw_id" {
  value = aws_apigatewayv2_api.main.id
}

output "spring_datasource_url_pedidos" {
  value = "jdbc:postgresql://${aws_db_instance.pedidos.address}:5432/${var.db_name_pedidos}?ssl=true&sslmode=require"
}

output "spring_datasource_url_producto" {
  value = "jdbc:postgresql://${aws_db_instance.producto.address}:5432/${var.db_name_producto}?ssl=true&sslmode=require"
}

output "spring_datasource_url_admin" {
  description = "Datasource de rabbitmq_admin_service: base logica propia en la misma instancia RDS de pedidos"
  value       = "jdbc:postgresql://${aws_db_instance.pedidos.address}:5432/${var.db_name_admin}?ssl=true&sslmode=require"
}

output "spring_datasource_username_pedidos" {
  value = var.db_username_pedidos
}

output "spring_datasource_username_producto" {
  value = var.db_username_producto
}

output "spring_datasource_password_pedidos" {
  value     = var.db_password
  sensitive = true
}

output "spring_datasource_password_producto" {
  value     = local.db_password_producto_eff
  sensitive = true
}

# ---------- Kafka ----------
output "ec2_kafka_public_ips" {
  description = "IPs publicas de los 3 brokers Kafka (SSH y Kafka UI)"
  value       = aws_instance.kafka[*].public_ip
}

output "ec2_kafka_private_ips" {
  description = "IPs privadas de los 3 brokers Kafka (bootstrap interno)"
  value       = aws_instance.kafka[*].private_ip
}

output "ec2_kafka_instance_ids" {
  description = "IDs de las EC2 Kafka (para pruebas de caida controlada)"
  value       = aws_instance.kafka[*].id
}

output "kafka_bootstrap_servers" {
  description = "Bootstrap servers privados para producto-service y analytics-service"
  value       = join(",", [for ip in aws_instance.kafka[*].private_ip : "${ip}:9092"])
}

output "kafka_ui_url" {
  description = "Kafka UI en el broker 3 (solo accesible desde admin_cidr)"
  value       = "http://${aws_instance.kafka[2].public_ip}:8080"
}
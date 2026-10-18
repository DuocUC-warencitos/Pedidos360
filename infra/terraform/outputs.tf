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
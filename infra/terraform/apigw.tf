resource "aws_apigatewayv2_api" "main" {
  name          = "${var.project}-api"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = split(",", var.cors_origins)
    allow_methods = ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
    allow_headers = ["Authorization", "Content-Type", "Idempotency-Key"]
    max_age       = 300
  }

  tags = { Name = "${var.project}-api" }
}

resource "aws_apigatewayv2_authorizer" "jwt" {
  api_id           = aws_apigatewayv2_api.main.id
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  name             = "entra-jwt"

  jwt_configuration {
    audience = [var.jwt_audience]
    issuer   = var.jwt_issuer_uri
  }
}

# Integración HTTP_PROXY a EC2 pedidos (pública para lab simple; si VPC Link, cambiar a VPC_LINK)
resource "aws_apigatewayv2_integration" "pedidos" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${aws_instance.pedidos.public_ip}:8080/api/v1/pedidos/{proxy}"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_integration" "productos" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${aws_instance.producto.public_ip}:8080/api/v1/productos/{proxy}"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_route" "pedidos" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /api/v1/pedidos/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.pedidos.id}"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt.id
  authorization_type = "JWT"
}

resource "aws_apigatewayv2_route" "productos" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /api/v1/productos/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.productos.id}"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt.id
  authorization_type = "JWT"
}

# Ruta health sin auth para ALB checks
resource "aws_apigatewayv2_route" "health" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /actuator/health"
  target    = "integrations/${aws_apigatewayv2_integration.pedidos.id}"
}

resource "aws_apigatewayv2_stage" "prod" {
  api_id      = aws_apigatewayv2_api.main.id
  name        = "prod"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.apigw.arn
    format          = "$context.requestId $context.identity.sourceIp $context.requestTime $context.httpMethod $context.routeKey $context.status"
  }

  tags = { Name = "${var.project}-prod" }
}

resource "aws_cloudwatch_log_group" "apigw" {
  name              = "/aws/apigateway/${var.project}"
  retention_in_days = 7
}

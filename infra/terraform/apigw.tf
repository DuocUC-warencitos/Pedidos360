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

# ──────────────────────────────────────────────────────────────
# PEDIDOS
# ──────────────────────────────────────────────────────────────

# Integración para la RAÍZ: /api/v1/pedidos (sin nada después)
resource "aws_apigatewayv2_integration" "pedidos_root" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${aws_instance.pedidos.public_ip}:8080/api/v1/pedidos"
  payload_format_version = "1.0"
}

# Integración para SUBRUTAS: /api/v1/pedidos/<algo>
resource "aws_apigatewayv2_integration" "pedidos" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${aws_instance.pedidos.public_ip}:8080/api/v1/pedidos/{proxy}"
  payload_format_version = "1.0"
}

# Ruta RAÍZ: POST/GET /api/v1/pedidos
resource "aws_apigatewayv2_route" "pedidos_root" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /api/v1/pedidos"
  target             = "integrations/${aws_apigatewayv2_integration.pedidos_root.id}"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt.id
  authorization_type = "JWT"
}

# Ruta SUBRUTAS: DELETE /api/v1/pedidos/all, etc.
resource "aws_apigatewayv2_route" "pedidos" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /api/v1/pedidos/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.pedidos.id}"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt.id
  authorization_type = "JWT"
}

# ──────────────────────────────────────────────────────────────
# PRODUCTOS
# ──────────────────────────────────────────────────────────────

resource "aws_apigatewayv2_integration" "productos_root" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${aws_instance.producto.public_ip}:8080/api/v1/productos"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_integration" "productos" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${aws_instance.producto.public_ip}:8080/api/v1/productos/{proxy}"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_route" "productos_root" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /api/v1/productos"
  target             = "integrations/${aws_apigatewayv2_integration.productos_root.id}"
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

# ──────────────────────────────────────────────────────────────
# HEALTH (sin auth)
# ──────────────────────────────────────────────────────────────

resource "aws_apigatewayv2_integration" "health" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${aws_instance.pedidos.public_ip}:8080/actuator/health"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_route" "health" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /actuator/health"
  target    = "integrations/${aws_apigatewayv2_integration.health.id}"
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

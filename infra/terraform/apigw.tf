resource "aws_apigatewayv2_api" "main" {
  name          = "${var.project}-api"
  protocol_type = "HTTP"
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

# Catálogo público: GET sin JWT (la ruta con método específico gana a la ANY)
resource "aws_apigatewayv2_route" "productos_root_get" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /api/v1/productos"
  target    = "integrations/${aws_apigatewayv2_integration.productos_root.id}"
}

resource "aws_apigatewayv2_route" "productos_get" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "GET /api/v1/productos/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.productos.id}"
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

# ──────────────────────────────────────────────────────────────
# RUTAS OPTIONS (preflight CORS) — SIN autorización
# ──────────────────────────────────────────────────────────────

# Para subrutas: /api/v1/pedidos/*
resource "aws_apigatewayv2_route" "pedidos_options" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "OPTIONS /api/v1/pedidos/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.pedidos.id}"
  # Sin authorizer_id ni authorization_type → NONE
}

# Para raíz: /api/v1/pedidos
resource "aws_apigatewayv2_route" "pedidos_root_options" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "OPTIONS /api/v1/pedidos"
  target    = "integrations/${aws_apigatewayv2_integration.pedidos_root.id}"
}

# Para subrutas: /api/v1/productos/*
resource "aws_apigatewayv2_route" "productos_options" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "OPTIONS /api/v1/productos/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.productos.id}"
}

# Para raíz: /api/v1/productos
resource "aws_apigatewayv2_route" "productos_root_options" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "OPTIONS /api/v1/productos"
  target    = "integrations/${aws_apigatewayv2_integration.productos_root.id}"
}

# ──────────────────────────────────────────────────────────────
# ADMIN (pantallas de administracion del front: /v1/admin/*)
# ──────────────────────────────────────────────────────────────

# Admin pedidos: /api/v1/admin/pedidos
resource "aws_apigatewayv2_integration" "admin_pedidos_root" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${aws_instance.pedidos.public_ip}:8080/api/v1/admin/pedidos"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_integration" "admin_pedidos" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${aws_instance.pedidos.public_ip}:8080/api/v1/admin/pedidos/{proxy}"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_route" "admin_pedidos_root" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /api/v1/admin/pedidos"
  target             = "integrations/${aws_apigatewayv2_integration.admin_pedidos_root.id}"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt.id
  authorization_type = "JWT"
}

resource "aws_apigatewayv2_route" "admin_pedidos" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /api/v1/admin/pedidos/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.admin_pedidos.id}"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt.id
  authorization_type = "JWT"
}

resource "aws_apigatewayv2_route" "admin_pedidos_options" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "OPTIONS /api/v1/admin/pedidos"
  target    = "integrations/${aws_apigatewayv2_integration.admin_pedidos_root.id}"
}

resource "aws_apigatewayv2_route" "admin_pedidos_sub_options" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "OPTIONS /api/v1/admin/pedidos/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.admin_pedidos.id}"
}

# Admin productos: /api/v1/admin/productos
resource "aws_apigatewayv2_integration" "admin_productos_root" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${aws_instance.producto.public_ip}:8080/api/v1/admin/productos"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_integration" "admin_productos" {
  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${aws_instance.producto.public_ip}:8080/api/v1/admin/productos/{proxy}"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_route" "admin_productos_root" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /api/v1/admin/productos"
  target             = "integrations/${aws_apigatewayv2_integration.admin_productos_root.id}"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt.id
  authorization_type = "JWT"
}

resource "aws_apigatewayv2_route" "admin_productos" {
  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY /api/v1/admin/productos/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.admin_productos.id}"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt.id
  authorization_type = "JWT"
}

resource "aws_apigatewayv2_route" "admin_productos_options" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "OPTIONS /api/v1/admin/productos"
  target    = "integrations/${aws_apigatewayv2_integration.admin_productos_root.id}"
}

resource "aws_apigatewayv2_route" "admin_productos_sub_options" {
  api_id    = aws_apigatewayv2_api.main.id
  route_key = "OPTIONS /api/v1/admin/productos/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.admin_productos.id}"
}

# ──────────────────────────────────────────────────────────────
# RABBIT ADMIN (notificaciones + panel admin/metricas)
# ──────────────────────────────────────────────────────────────

locals {
  rabbit_admin_base = "http://${aws_instance.rabbit_admin.public_ip}:8080"
  rabbit_admin_paths = {
    notificaciones = "/api/notificaciones"
    admin_rabbit   = "/api/admin/rabbit"
    admin_metricas = "/api/admin/metricas"
  }
}

resource "aws_apigatewayv2_integration" "rabbit_admin_root" {
  for_each = local.rabbit_admin_paths

  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "${local.rabbit_admin_base}${each.value}"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_integration" "rabbit_admin_sub" {
  for_each = local.rabbit_admin_paths

  api_id                 = aws_apigatewayv2_api.main.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "${local.rabbit_admin_base}${each.value}/{proxy}"
  payload_format_version = "1.0"
}

resource "aws_apigatewayv2_route" "rabbit_admin_root" {
  for_each = local.rabbit_admin_paths

  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY ${each.value}"
  target             = "integrations/${aws_apigatewayv2_integration.rabbit_admin_root[each.key].id}"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt.id
  authorization_type = "JWT"
}

resource "aws_apigatewayv2_route" "rabbit_admin_sub" {
  for_each = local.rabbit_admin_paths

  api_id             = aws_apigatewayv2_api.main.id
  route_key          = "ANY ${each.value}/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.rabbit_admin_sub[each.key].id}"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt.id
  authorization_type = "JWT"
}

resource "aws_apigatewayv2_route" "rabbit_admin_options" {
  for_each = local.rabbit_admin_paths

  api_id    = aws_apigatewayv2_api.main.id
  route_key = "OPTIONS ${each.value}/{proxy+}"
  target    = "integrations/${aws_apigatewayv2_integration.rabbit_admin_sub[each.key].id}"
}

resource "aws_apigatewayv2_route" "rabbit_admin_root_options" {
  for_each = local.rabbit_admin_paths

  api_id    = aws_apigatewayv2_api.main.id
  route_key = "OPTIONS ${each.value}"
  target    = "integrations/${aws_apigatewayv2_integration.rabbit_admin_root[each.key].id}"
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

# infra/envs - Esquemas y secretos de despliegue

Una carpeta por servicio/EC2 con el **esquema del `.env` final** que se sube al servidor.
Los scripts de `infra/scripts/` construyen ese `.env` combinando:

1. **AUTOMATICO**: outputs de terraform (URLs/usuarios/passwords de RDS, IPs privadas) e imagenes GHCR por defecto.
2. **comun.env**: secretos compartidos (key SSH, credenciales RabbitMQ, JWT).
3. **Overrides por servicio**: si existe `infra/envs/<servicio>/.env`, sus claves pisan lo anterior.

## Flujo

```powershell
# Opcion A: todo automatico (recomendado)
.\infra\scripts\deploy-all.ps1 -AutoApprove
#   - crea infra/envs/comun.env si no existe (desde comun.env.example)
#   - genera solo RABBITMQ_PASSWORD, RABBITMQ_ERLANG_COOKIE e INTERNAL_API_KEY si estan vacios
#   - corre terraform apply y despliega las 4 EC2

# Opcion B: manual
Copy-Item infra\envs\comun.env.example infra\envs\comun.env
notepad infra\envs\comun.env   # completa AWS_KEY_PATH y lo que quieras fijar
.\infra\scripts\deploy-all.ps1
```

## Overrides puntuales por servicio

Copiar el `.env.example` de la carpeta a `.env` (mismo directorio) y editar solo lo necesario:

```powershell
Copy-Item infra\envs\rabbit-admin\.env.example infra\envs\rabbit-admin\.env
# editar infra\envs\rabbit-admin\.env (ej: usar Supabase en vez del RDS admin)
```

## CORS

- **AWS**: los servicios van con `CORS_ENABLED=true` (default del backend) y `CORS_ALLOWED_ORIGINS` del front (se define en `comun.env`, varios separados por coma; actual: `https://pedidos360-three.vercel.app`).
- **Local** (`infra/docker/docker-compose.yml`): los servicios van con `CORS_ENABLED=false`; el `pedidos-gateway` local maneja CORS.

## Seguridad servicio-a-servicio (pedidos -> producto)

- `INTERNAL_API_KEY` (comun.env, autogenerada) viaja en el `.env` de pedidos y producto.
- pedidos envia el header `X-Internal-Api-Key` en cada llamada Feign; producto lo exige en
  `/api/v1/productos/stock/**` y `/api/v1/productos/detalles` (403 si falta o no coincide).
- Red: pedidos llama por **IP privada** y el SG de producto solo abre 8080 desde el SG de pedidos
  (el 8080 publico restante es el que necesita API Gateway).
- Local: la clave viene del `.env` versionado de `infra/docker`.

## GHCR privado (opcional)

Si los paquetes de `ghcr.io/duocuc-warencitos/*` son privados, define en `comun.env`:
`GHCR_USERNAME` (tu usuario GitHub) y `GHCR_TOKEN` (PAT clasico con `read:packages`).
Los deploy scripts de rabbit-admin/producto/pedidos hacen `docker login ghcr.io` en la EC2
antes del `docker compose up`. Si los paquetes son publicos, deja ambos vacios.

## Git

- `infra/envs/comun.env`, `infra/envs/aws.env` y `infra/envs/*/.env` estan **gitignored** (secretos locales).
- Los `.env.example` se commitean (esquema).
- La password de RabbitMQ generada queda en `comun.env` (abrilo para verla antes de entrar a la Management UI).

## Carpetas

| Carpeta | EC2 | `.env` final en el server |
|---|---|---|
| `rabbit-cluster/` | cluster rabbit1 + rabbit2 | `/opt/rabbit-cluster/.env` |
| `rabbit-admin/` | rabbit-admin-service | `/opt/rabbit-admin/.env` |
| `pedidos-service/` | pedidos-service | `/opt/pedidos360/.env` |
| `producto-service/` | producto-service | `/opt/pedidos360/.env` |

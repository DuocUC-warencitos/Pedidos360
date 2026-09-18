# Pedidos360 — Guía Saga Distribuida REST `202 + polling`

> Sistema distribuido semestral: `pedidos-service` (8081) <-> `producto-service` (8082) vía REST sin mensajería, estados `CREADO->CONFIRMADO->EN_PREPARACION->DESPACHADO->ENTREGADO/CANCELADO`, stock verificado/descontado/devuelto, endpoint inter-servicio **abierto** (`permitAll`), frontend Angular polling como `temp-monitor` `compaction_jobs`.

Inspirado en tu `temp-monitor-api/docs/async-compactacion.md` (`POST 202 {id,RUNNING} -> GET /{id} polling cada 2s`).

---

## 0. Por qué no todo en una petición

- **No hay ACID distribuido**: `pedidos` y `producto` tienen DBs distintas (`Supabase` pooler `pedidos-service/src/main/resources/application.yml:6` = misma URL hoy, pero deben ser DB per service). Un `POST /pedidos` que haga `INSERT pedido` + `UPDATE producto.stock` en una sola transacción no existe (2PC no se usa).
- **Bloqueo**: Si `producto-service` tarda o cae, el `POST` queda colgado >30s -> `504 Gateway` (`pedidos-gateway/src/main/resources/application.yml`).
- **Solución**: `202 Accepted` desacopla: `pedidos-service` guarda `CREADO` local (rápido, <100ms) y luego orquesta `reservar stock` async; el frontend hace `GET /pedidos/{id}` polling 3s hasta `CONFIRMADO` o `CANCELADO` (STOCK_FALLIDO). Permite **compensar** (`liberar` stock si el pedido falla después).

---

## 1. Estado hoy (qué falla)

| Archivo | Línea | Problema |
|---|---|---|
| `backend/pedidos-service/src/main/java/.../model/Pedido.java:102` | `List<PedidoProducto>` con `nombreProducto` libre | Sin `productoId` UUID, sin `precioSnapshot`, no correlaciona con `producto-service` |
| `backend/producto-service/src/main/java/.../model/Producto.java:38` | `id, nombre, precio, @Version` | **Sin `stock`**: `grep stock` = 0 en todo `producto-service` |
| `backend/pedidos-service/src/main/java/.../core/application/PedidoService.java:78` | `save()` solo `pedidoRepository.save()` | 0 llamadas a `producto-service` (`grep RestClient|Feign|8082` = 0) |
| `backend/producto-service/src/main/java/.../security/SecurityConfig.java:37` | `anyRequest().permitAll()` | Todo público, pero no hay `POST /stock/**` que proteger |
| `frontend/src/app/features/pedidos/data/pedidos.queries.ts:81` | `usePedidosQuery` sin `refetchInterval` | No hay polling, solo `invalidate` tras mutación |
| `frontend/src/app/features/pedidos/pages/pedidos-create/pedidos-create.ts:100` | `signal productos: PedidoProductoRequest[]` | Texto libre, no selector de catálogo `productos.service.ts:51` |

---

## 2. Arquitectura objetivo (orquestador embebido, REST)

```
[Angular pedidos-create] --POST /api/v1/pedidos {items:[{productoId,cantidad}]} + Idempotency-Key--> [Gateway:8080] --> [pedidos-service:8081]
   | 202 {id, CREADO, correlationId}                                            |
   |<--poll GET /api/v1/pedidos/{id} 3s (TanStack Query)------------------------| 
   |  CREADO -> STOCK_RESERVADO -> CONFIRMADO / CANCELADO (STOCK_FALLIDO)       v
   |                                                                    [PedidoService.orchestrate]
   |                                                                      1. TX1: Pedido CREADO + PedidoJob RUNNING
   |                                                                      2. REST -> producto-service POST /stock/reservar {pedidoId, items} Idempotency-Key=pedidoId
   |                                                                      3a. 200 -> Pedido STOCK_RESERVADO -> CONFIRMADO
   |                                                                      3b. 409 STOCK_INSUFFICIENTE -> Pedido CANCELADO + compensar
   |                                                                      4. Frontend polling ve transición
```

Reutiliza tu `temp-monitor-api/src/main/java/.../CompactionJobService.java:27` (`ConcurrentHashMap` o JPA `compaction_jobs`).

---

## 3. Fase 1 — Modelo `stock` en `producto-service` (1 día)

**Qué tocar:**
- `backend/producto-service/src/main/java/.../model/Producto.java`
- `V2__add_stock.sql` (Flyway) o `ddl-auto:update`

**Qué hacer:**
```java
// Producto.java
@Column(nullable=false) int stockDisponible = 100;
@Column(nullable=false) int stockReservado = 0;
```
O tabla separada `Stock` + `ReservaStock` (`pedidoId, productoId, cantidad, idempotencyKey UNIQUE, estado PENDIENTE/CONFIRMADA/LIBERADA, expiresAt`).

**Por qué:** sin `stockDisponible` no hay `verificar/descontar/devolver`.

**Prompt DeepSeek 1 (copiar/pegar):**
> Actúa como arquitecto Spring Boot 4 + Java 21. En `C:\...\producto-service\src\main\java\...\Producto.java` [pega contenido actual] agrega `stockDisponible` y `stockReservado` con `@Version` existente para optimistic locking. Genera solo el diff y el SQL Flyway `V2__add_stock.sql` con `DEFAULT 0`. No toques `ProductoController.java` ni `SecurityConfig.java`. Explica en 2 líneas por qué `@Version` evita stock negativo concurrente.

---

## 4. Fase 2 — Endpoints stock idempotentes `producto-service` (1 día)

**Qué crear:**
- `StockController.java` `POST /api/v1/productos/stock/verificar|reservar|liberar|confirmar` **abierto** (`permitAll` como pediste, sin JWT)
- DTOs `ReservaStockRequest {UUID pedidoId, List<Item{productoId,cantidad}>, String idempotencyKey}`, `ReservaStockResponse`

**Lógica `reservar` `@Transactional`:**
```java
if(reservaRepository.existsByIdempotencyKey(key)) return findByKey(key); // idempotencia
for(Item i: req.items()){
  Producto p = productoRepository.findById(i.productoId()).orElseThrow();
  if(p.getStockDisponible() < i.cantidad()) throw new StockInsuficienteException(...); // -> 409
  p.setStockDisponible(p.getStockDisponible() - i.cantidad());
  p.setStockReservado(p.getStockReservado() + i.cantidad());
}
```

**Por qué abierto:** simplifica `pedidos-service` (no propaga JWT). Riesgo prod: documentar `TODO: hasAuthority('SCOPE_stock:write')`.

**Prompt 2:**
> Crea `StockController` en `producto-service` con `POST /api/v1/productos/stock/reservar` idempotente por `Idempotency-Key` header. Usa `ReservaStockRepository.findByIdempotencyKey`. Si ya existe retorna 200 anterior. Si no, descuenta `stockDisponible` y aumenta `stockReservado` en transacción, guarda `ReservaStock`. Lanza `StockInsuficienteException` mapeada a 409 vía `@ControllerAdvice`. Solo genera controller y service, deja `SecurityConfig.java` con `permitAll`.

---

## 5. Fase 3 — `PedidoProducto` + `Pedido` en `pedidos-service` (0.5 día)

**Qué tocar:**
- `PedidoProducto.java:31` `String nombreProducto` → `UUID productoId + String nombreSnapshot + int precioSnapshot`
- `Pedido.java:102` agregar `String idempotencyKey UNIQUE, String correlationId, @Version Long version` y estados `STOCK_RESERVADO, STOCK_FALLIDO`

**Por qué:** correlacionar y snapshot histórico.

**Prompt 3:**
> Refactoriza `PedidoProducto.java` [pega] para que guarde `productoId` UUID + snapshot `nombre` y `precio` al momento del pedido. Mantén compatibilidad con `PedidoResponseMapper.java:37`. Genera solo el diff y explica por qué snapshot evita que cambio de precio en `producto-service` afecte pedidos históricos.

---

## 6. Fase 4 — Cliente REST en `pedidos-service` (0.5 día)

**Qué tocar:**
- `pom.xml:78` + `spring-cloud-starter-openfeign` o `spring-boot-starter-restclient` + `resilience4j-spring-boot4`
- `infrastructure/client/ProductoClient.java` Feign `url=${app.producto-service.url:http://localhost:8082}`

**Por qué:** orquestador necesita llamar sin gateway.

**Prompt 4:**
> Crea `ProductoClient` Feign en `pedidos-service` con `application.yml` `app.producto-service.url`. Endpoints `POST /stock/reservar` con `@RequestHeader("Idempotency-Key")` y `GET /{id}`. Añade `@Retry @CircuitBreaker(name="productoService") @TimeLimiter` de Resilience4j con `failureRateThreshold 50%`. No uses WebClient.

---

## 7. Fase 5 — Saga orquestada `202 + polling` (2 días, corazón)

**Qué tocar:**
- `PedidoJob.java` (`id, estado RUNNING/COMPLETED/FAILED, iniciadoEn, ejecutadoPorId, correlationId`) + `PedidoJobRepository`
- `PedidoJobService` `@Async("pedidoExecutor")` (copia `temp-monitor-api/src/main/java/.../CompactionJobService.java:27` con `ThreadPoolTaskExecutor` `pedido-` de `AsyncConfig.java`)
- `PedidoService.save()` → `crearPedidoSaga()`: `TX1` guarda `CREADO`, fuera de `TX` llama `productoClient.reservar()`, `TX2` actualiza a `CONFIRMADO` o `CANCELADO`. `cancelar()` compensa `productoClient.liberar(pedidoId)`.

**Por qué:** evita todo en una petición, permite `liberar` si falla después, frontend ve `CREADO->CONFIRMADO` progresivo.

**Prompt 5a (lógica transaccional):**
> En `PedidoService.java` [pega 78 líneas] refactoriza `save()` a `crearPedidoSaga()` que no llame a `pedidoRepository.save()` y luego a `productoClient` dentro de la misma transacción. Separa: TX1 guarda `CREADO`, fuera de TX llama `reservar`, TX2 actualiza a `CONFIRMADO/CANCELADO`. Usa `TransactionTemplate`. Explica por qué no debe ser una sola `@Transactional`.

**Prompt 5b (async job):**
> Crea `PedidoJobService` async con `ConcurrentHashMap` o JPA, con `crearJob(TokenUser)` y `ejecutarAsync(jobId)` que hace `productoClient.reservar` + `pedidoRepository.save`. Guarda `quien` de `SpringSecurityUserTokenService.java:55`. Usa `ApplicationContext` para evitar self-invocation de `@Async`.

---

## 8. Fase 6 — Frontend Angular polling + timeline (1 día)

**Qué tocar:**
- `frontend/src/app/features/pedidos/data/pedidos.queries.ts:81` agregar `refetchInterval: (q)=> q.state.data?.some(p=>!['ENTREGADO','CANCELADO'].includes(p.estado))?3000:false` + `refetchOnWindowFocus:true`
- `pedidos-create.ts:100` cambiar `nombreProducto` input → `select` de `productos.service.ts:51` (`obtenerProductos()`), enviar `productoId`
- `ui/pedido-timeline` nuevo (stepper 5 pasos reutilizando `ui-badge.ts` colores) para espejar `temp-monitor-frontend/src/features/admin/pages/AdminPage.tsx`
- `pedidos-create.html:156` mostrar `estado` + timeline, `pedidos-list.html` polling skeleton

**Por qué:** espejar `compaction_jobs` polling.

**Prompt 6:**
> En `frontend/src/app/features/pedidos/data/pedidos.queries.ts` [pega] agrega polling condicional 3s solo si hay pedidos no terminales. No toques `pedidos.service.ts`. Solo genera el diff y explica cómo `invalidateQueries(['pedidos'])` complementa el polling.

---

## 9. Fase 7 — Gateway, Docker y seguridad abierta (0.5 día)

- `pedidos-gateway/.../application.yml:27` ya rutea, pero inter-servicio debe ir directo `http://producto-service:8082` (no por gateway) configurado en `application.yml: app.producto-service.url`.
- `producto-service/SecurityConfig.java:37` deja `requestMatchers("/api/v1/productos/stock/**").permitAll()` (abierto) + `GET /productos/**` `permitAll`.
- `docker-compose.yml` raíz que levanta `pedidos-service:8081`, `producto-service:8082`, `gateway:8080`, `postgres:15`, `frontend:4200`.

**Prompt 7:**
> Genera `docker-compose.yml` raíz para Pedidos360 con 4 servicios, env `DATABASE_URL` y `JWT_ISSUER_URI` ya existentes, sin tocar `application.yml` de cada servicio. Usa `postgres:15` con `pedidos-data`.

---

## 10. Fase 8 — Tests y defensa (0.5 día)

- `Testcontainers PostgreSQL` + `WireMock` para `ProductoClient` (feliz/409/compensa), `EstadoPedidoTest`, `PedidoServiceSagaTest`.
- `frontend` `vitest` para `pedido-timeline`.

**Prompt 8:**
> Genera test `PedidoServiceSagaTest` con WireMock que simula `POST /stock/reservar` 200 y 409, verifica que `pedido.estado` queda `CONFIRMADO` o `CANCELADO` y que `liberar` se llama al cancelar. Usa Testcontainers.

---

## Cómo usar los prompts con DeepSeek

1. **Siempre pega:** `Ruta absoluta + contenido del archivo` + `qué no tocar` + `solo diff` + `por qué`.
2. **Ejecuta fase por fase**, compila `mvn verify` y `ng build` tras cada prompt, no pidas todo de golpe.
3. **Valida:** `mvn test -Dtest=PedidoServiceSagaTest`, `curl -H "Idempotency-Key: pedido-123" POST /stock/reservar` dos veces → segunda debe retornar mismo 200 (idempotencia).
4. **Itera:** si DeepSeek genera `switch` sobre `EstadoPedido`, recuérdale tu `EstadoPedido.java:49` ya tiene `puedeTransicionarA()` — no agregar `switch`.

---

## Check final distribuido

- [ ] `productoId` + `stock` en DB + `409` cuando `disponible < cantidad`
- [ ] `POST /pedidos` retorna `202` + `GET /pedidos/{id}` polling ve `CREADO → CONFIRMADO`
- [ ] `cancelar` libera stock (compensación REST idempotente)
- [ ] `Idempotency-Key` header evita doble descuento
- [ ] Frontend selector + timeline + polling 3s
- [ ] `docker-compose up` levanta los 4 + gateway
- [ ] `mvn verify` verde sin `switch` nuevo

¿Empezamos por Fase 1 (stock) pegando el Prompt 1 a DeepSeek?

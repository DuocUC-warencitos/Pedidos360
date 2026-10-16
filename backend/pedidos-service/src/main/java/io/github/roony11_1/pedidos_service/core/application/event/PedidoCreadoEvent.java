package io.github.roony11_1.pedidos_service.core.application.event;

import java.time.Instant;
import java.util.UUID;

public record PedidoCreadoEvent(
    UUID jobId,
    UUID pedidoId,
    String userId,
    String idempotencyKey,
    String correlationId,
    Instant occurredAt
) {}

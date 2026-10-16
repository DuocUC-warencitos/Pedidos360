package io.github.roony11_1.pedidos_service.core.application.event;

import java.time.Instant;
import java.util.UUID;

public record PedidoStockFallidoEvent(
    UUID jobId,
    UUID pedidoId,
    String userId,
    String correlationId,
    String error,
    Instant occurredAt
) {}

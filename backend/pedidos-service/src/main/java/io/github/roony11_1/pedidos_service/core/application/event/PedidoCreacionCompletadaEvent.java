package io.github.roony11_1.pedidos_service.core.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PedidoCreacionCompletadaEvent(
    UUID jobId,
    UUID pedidoId,
    String userId,
    String correlationId,
    Instant occurredAt,
    List<Item> items
) {
    public record Item(
        UUID productoId,
        int cantidad
    ) {}
}

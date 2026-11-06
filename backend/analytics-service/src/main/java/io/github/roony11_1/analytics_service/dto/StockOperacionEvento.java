package io.github.roony11_1.analytics_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StockOperacionEvento(
    UUID eventoId,
    int schemaVersion,
    String tipo,
    String operacionId,
    UUID pedidoId,
    String idempotencyKey,
    String correlationId,
    Instant ocurridoEn,
    String origen,
    List<StockItemEvento> items
) {
}

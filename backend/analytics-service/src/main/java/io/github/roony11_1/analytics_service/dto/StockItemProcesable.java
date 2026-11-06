package io.github.roony11_1.analytics_service.dto;

import java.time.Instant;
import java.util.UUID;

public record StockItemProcesable(
    UUID eventoId,
    String tipo,
    String operacionId,
    UUID pedidoId,
    String correlationId,
    Instant ocurridoEn,
    StockItemEvento item
) {
}

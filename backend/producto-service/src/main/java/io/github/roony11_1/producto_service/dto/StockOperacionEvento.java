package io.github.roony11_1.producto_service.dto;

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
    public static final int SCHEMA_VERSION = 1;
    public static final String ORIGEN = "producto-service";
}

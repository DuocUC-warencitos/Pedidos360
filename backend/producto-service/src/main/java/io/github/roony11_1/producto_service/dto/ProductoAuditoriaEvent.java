package io.github.roony11_1.producto_service.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductoAuditoriaEvent(
    UUID eventoId,
    int schemaVersion,
    String tipo,
    UUID productoId,
    Instant ocurridoEn,
    String usuarioId,
    String correlationId,
    Long version,
    ProductoEvento antes,
    ProductoEvento despues
) {
    public static final int SCHEMA_VERSION = 1;
}

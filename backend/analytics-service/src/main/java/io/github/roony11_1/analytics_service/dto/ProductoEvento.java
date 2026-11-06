package io.github.roony11_1.analytics_service.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductoEvento(
    UUID id,
    int precio,
    String nombre,
    String sku,
    String categoria,
    String descripcion,
    String shortDescription,
    int stockDisponible,
    int stockReservado,
    Integer stockMinimo,
    boolean activo,
    Instant createdAt,
    Instant updatedAt,
    Long version
) {
}

package io.github.roony11_1.analytics_service.streams;

import java.time.Instant;
import java.util.UUID;

public record InventarioProducto(
    UUID productoId,
    String nombre,
    String sku,
    String categoria,
    int precio,
    int stockDisponible,
    int stockReservado,
    Integer stockMinimo,
    boolean activo,
    Instant actualizadoEn,
    Long version
) {
}

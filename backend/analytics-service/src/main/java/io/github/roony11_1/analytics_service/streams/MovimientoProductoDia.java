package io.github.roony11_1.analytics_service.streams;

import java.util.UUID;

public record MovimientoProductoDia(
    UUID productoId,
    String nombre,
    String sku,
    String fecha,
    String tipo,
    long unidades,
    long ingresos,
    long operaciones
) {
}

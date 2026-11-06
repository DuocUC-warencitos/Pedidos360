package io.github.roony11_1.analytics_service.dto;

import java.util.UUID;

public record TopProductoResponse(
    UUID productoId,
    String nombre,
    String sku,
    long unidades,
    long ingresos) 
{
}

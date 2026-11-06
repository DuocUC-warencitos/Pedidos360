package io.github.roony11_1.analytics_service.dto;

import java.util.UUID;

public record StockItemEvento(
    UUID productoId,
    String nombre,
    String sku,
    String categoria,
    int precioUnitario,
    int cantidad,
    int stockDisponibleAntes,
    int stockDisponibleDespues,
    int stockReservadoAntes,
    int stockReservadoDespues
) {
}

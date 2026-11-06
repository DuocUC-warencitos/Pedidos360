package io.github.roony11_1.analytics_service.streams;

import java.time.Instant;
import java.util.UUID;

public record AuditoriaItem(
    UUID eventoId,
    String tipo,
    UUID productoId,
    String nombre,
    String sku,
    UUID pedidoId,
    String operacionId,
    int cantidad,
    Integer stockDisponibleAntes,
    Integer stockDisponibleDespues,
    Integer stockReservadoAntes,
    Integer stockReservadoDespues,
    Instant ocurridoEn,
    String correlationId,
    String usuarioId
) {
}

package io.github.roony11_1.analytics_service.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditoriaItemResponse(
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
    String usuarioId) 
{
}

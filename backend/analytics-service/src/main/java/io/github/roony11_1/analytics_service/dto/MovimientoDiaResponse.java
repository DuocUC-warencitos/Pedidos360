package io.github.roony11_1.analytics_service.dto;

public record MovimientoDiaResponse(
    String fecha,
    String tipo,
    long unidades,
    long ingresos,
    long operaciones) 
{
}

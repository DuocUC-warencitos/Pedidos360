package io.github.roony11_1.analytics_service.dto;

public record ResumenAnaliticaResponse(
    long total,
    long activos,
    long stockTotal,
    long stockReservadoTotal,
    long alertas,
    long unidadesConfirmadasMes,
    long ingresosConfirmadosMes) 
{
}

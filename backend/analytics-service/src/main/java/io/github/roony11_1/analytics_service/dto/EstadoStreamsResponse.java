package io.github.roony11_1.analytics_service.dto;

import java.util.List;

public record EstadoStreamsResponse(
    String estado,
    List<String> stores,
    long productos,
    long movimientos,
    long eventos) 
{
}

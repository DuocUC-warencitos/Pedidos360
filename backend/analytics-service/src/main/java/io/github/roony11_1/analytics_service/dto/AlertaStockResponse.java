package io.github.roony11_1.analytics_service.dto;

import java.util.UUID;

public record AlertaStockResponse(
    UUID id,
    String nombre,
    String sku,
    int stockDisponible,
    Integer stockMinimo) 
{
}

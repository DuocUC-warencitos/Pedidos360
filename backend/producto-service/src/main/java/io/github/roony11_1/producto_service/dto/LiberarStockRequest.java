package io.github.roony11_1.producto_service.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record LiberarStockRequest(
    @NotNull UUID pedidoId,
    @NotEmpty List<Item> items)
{
    public record Item(
        @NotNull UUID productoId,
        @Min(1) int cantidad)
    {
    }
}

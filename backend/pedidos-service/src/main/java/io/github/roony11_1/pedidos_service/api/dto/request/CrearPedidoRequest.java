package io.github.roony11_1.pedidos_service.api.dto.request;

import java.util.List;
import java.util.UUID;

import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CrearPedidoRequest(@NotEmpty List<Item> items) 
{
    public record Item(
            @NotNull UUID productoId,
            @Min(1) int cantidad) 
    {

    }

    public List<PedidoProducto> toProductos() 
    {
        return items.stream()
                .map(i -> PedidoProducto.builder()
                        .productoId(i.productoId())
                        .cantidad(i.cantidad())
                        // placeholders; se reemplazan en la saga antes de persistir
                        .nombreSnapshot("")
                        .precioSnapshot(0)
                        .build())
                .toList();
    }
}
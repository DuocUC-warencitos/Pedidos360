package io.github.roony11_1.pedidos_service.api.mapper;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import io.github.roony11_1.pedidos_service.api.dto.response.PedidoProductoResponse;
import io.github.roony11_1.pedidos_service.api.dto.response.PedidoResponse;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class PedidoResponseMapper implements Function<Pedido, PedidoResponse>
{
    private final Function<PedidoProducto, PedidoProductoResponse> pedidoProductoMapper = pedidoProducto ->
        PedidoProductoResponse.builder()
            .productoId(pedidoProducto.getProductoId())
            .nombreProducto(pedidoProducto.getNombreSnapshot())
            .cantidad(pedidoProducto.getCantidad())
            .build();

    @Override
    public PedidoResponse apply(Pedido entity) 
    {
        return PedidoResponse.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .productos(entity.getProductos().stream()
                .map(pedidoProductoMapper::apply).toList())
            .estado(entity.getEstadoPedido())
            .comentario(entity.getComentario())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}

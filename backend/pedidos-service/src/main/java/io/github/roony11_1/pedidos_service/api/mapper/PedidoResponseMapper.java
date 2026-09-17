package io.github.roony11_1.pedidos_service.api.mapper;

import org.springframework.stereotype.Component;

import io.github.roony11_1.pedidos_service.api.dto.response.PedidoProductoResponse;
import io.github.roony11_1.pedidos_service.api.dto.response.PedidoResponse;
import io.github.roony11_1.pedidos_service.core.domain.mapper.IMapper;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class PedidoResponseMapper implements IMapper<PedidoResponse, Pedido>
{
    private final IMapper<PedidoProductoResponse, PedidoProducto> pedidoProductoMapper = pedidoProducto ->
        PedidoProductoResponse.builder()
            .nombreProducto(pedidoProducto.getNombreProducto())
            .cantidad(pedidoProducto.getCantidad())
            .build();

    @Override
    public PedidoResponse map(Pedido entity) 
    {
        return PedidoResponse.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .productos(entity.getProductos().stream().map(pedidoProductoMapper::map).toList())
            .estado(entity.getEstadoPedido())
            .comentario(entity.getComentario())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}

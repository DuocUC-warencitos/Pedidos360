package io.github.roony11_1.pedidos_service.api.mapper;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import io.github.roony11_1.pedidos_service.api.dto.response.PedidoDetailResponse;
import io.github.roony11_1.pedidos_service.api.dto.response.PedidoProductoResponse;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class PedidoDetailResponseMapper implements Function<Pedido, PedidoDetailResponse>
{
    private final Function<PedidoProducto, PedidoProductoResponse> pedidoProductoMapper;

    @Override
    public PedidoDetailResponse apply(Pedido entity)
    {
        return PedidoDetailResponse.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .productos(entity.getProductos().stream()
                .map(pedidoProductoMapper)
                .toList())
            .estado(entity.getEstadoPedido())
            .comentario(entity.getComentario())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
package io.github.roony11_1.pedidos_service.api.mapper;

import org.springframework.stereotype.Component;

import io.github.roony11_1.pedidos_service.api.dto.response.PedidoResponse;
import io.github.roony11_1.pedidos_service.core.domain.mapper.IMapper;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor 
public class PedidoResponseMapper implements IMapper<PedidoResponse, Pedido>
{
    private final PedidoProductoResponseMapper mapper;


    @Override
    public PedidoResponse map(Pedido entity) 
    {
        return PedidoResponse.builder()
            .userId(entity.getUserId())
            .productos(entity.getProductos().stream().map(mapper::map).toList())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}

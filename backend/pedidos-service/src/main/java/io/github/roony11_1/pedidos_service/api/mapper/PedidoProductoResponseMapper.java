package io.github.roony11_1.pedidos_service.api.mapper;

import org.springframework.stereotype.Component;

import io.github.roony11_1.pedidos_service.api.dto.response.PedidoProductoResponse;
import io.github.roony11_1.pedidos_service.core.domain.mapper.IMapper;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;

@Component 
public class PedidoProductoResponseMapper implements IMapper<PedidoProductoResponse, PedidoProducto> 
{
    @Override
    public PedidoProductoResponse map(PedidoProducto entity) 
    {
        return PedidoProductoResponse.builder()
            .nombreProducto(entity.getNombreProducto())
            .cantidad(entity.getCantidad())
            .build();
    }
}


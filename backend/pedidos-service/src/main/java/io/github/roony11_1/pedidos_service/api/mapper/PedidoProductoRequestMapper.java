package io.github.roony11_1.pedidos_service.api.mapper;

import org.springframework.stereotype.Component;

import io.github.roony11_1.pedidos_service.api.dto.request.PedidoProductoRequest;
import io.github.roony11_1.pedidos_service.api.dto.response.PedidoProductoResponse;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;
import io.github.roony11_1.pedidos_service.core.domain.mapper.IMapper;

@Component
public class PedidoProductoRequestMapper implements IMapper<PedidoProducto, PedidoProductoRequest>
{
    public PedidoProducto map(PedidoProductoRequest request)
    {
        return PedidoProducto.builder()
            .nombreProducto(request.getNombreProducto())
            .cantidad(request.getCantidad())
            .build();
    }

    public PedidoProductoResponse map(PedidoProducto entity)
    {
        return PedidoProductoResponse.builder()
            .nombreProducto(entity.getNombreProducto())
            .cantidad(entity.getCantidad())
            .build();
    }
}

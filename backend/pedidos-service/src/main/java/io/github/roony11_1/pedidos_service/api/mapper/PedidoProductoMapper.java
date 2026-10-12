package io.github.roony11_1.pedidos_service.api.mapper;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import io.github.roony11_1.pedidos_service.api.dto.response.PedidoProductoResponse;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;

@Component 
public class PedidoProductoMapper implements Function<PedidoProducto, PedidoProductoResponse>
{
    @Override
    public PedidoProductoResponse apply(PedidoProducto pedidoProducto) 
    {
        return PedidoProductoResponse.builder()
            .productoId(pedidoProducto.getProductoId())
            .nombreProducto(pedidoProducto.getNombreSnapshot())
            .cantidad(pedidoProducto.getCantidad())
            .precio(pedidoProducto.getPrecioSnapshot())
            .build();     
    }
}

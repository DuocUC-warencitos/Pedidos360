package io.github.roony11_1.pedidos_service.api.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor
public class PedidoProductoRequest 
{
    private String nombreProducto;
    private int cantidad;
}

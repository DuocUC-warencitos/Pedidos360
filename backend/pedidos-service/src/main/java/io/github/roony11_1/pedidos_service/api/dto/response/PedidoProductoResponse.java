package io.github.roony11_1.pedidos_service.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class PedidoProductoResponse 
{
    private String nombreProducto;
    private int cantidad;
}

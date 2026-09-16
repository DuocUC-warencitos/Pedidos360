package io.github.roony11_1.pedidos_service.api.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.github.roony11_1.pedidos_service.core.domain.model.EstadoPedido;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class PedidoResponse 
{
    private UUID id;
    private String userId;
    private List<PedidoProductoResponse> productos;

    private EstadoPedido estado;

    private Instant createdAt;
    private Instant updatedAt;
}

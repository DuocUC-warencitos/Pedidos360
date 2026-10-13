package io.github.roony11_1.pedidos_service.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoStatsResponse
{
    private long total;
    private long cancelables;
    private long cancelados;
}

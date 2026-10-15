package io.github.roony11_1.producto_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoStatsResponse
{
    private long total;
    private long activos;
    private long stockTotal;
    private long stockReservadoTotal;
}

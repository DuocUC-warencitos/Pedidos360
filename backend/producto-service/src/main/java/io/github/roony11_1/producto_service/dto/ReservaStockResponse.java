package io.github.roony11_1.producto_service.dto;

import java.time.Instant;
import java.util.UUID;

import io.github.roony11_1.producto_service.model.EstadoReserva;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaStockResponse 
{
    private UUID reservaId;
    private UUID pedidoId;
    private EstadoReserva estado;
    private Instant createdAt;
    private boolean idempotente;
}

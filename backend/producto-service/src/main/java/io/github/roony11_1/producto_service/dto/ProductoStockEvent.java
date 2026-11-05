package io.github.roony11_1.producto_service.dto;

import java.time.Instant;
import java.util.UUID;

import io.github.roony11_1.producto_service.model.EstadoReserva;

public record ProductoStockEvent(
    UUID eventoId,
    EstadoReserva estadoReserva,
    UUID productoId,
    int stockFinal,
    int stockReservadoFinal,
    Instant fechaHora
) {}
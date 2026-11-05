package io.github.roony11_1.producto_service.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductoAuditoriaEvent(
    UUID eventoId,
    UUID productoId,
    String accion,
    Instant fechaHora,
    String usuarioId,
    ProductoEvento estadoAnterior,
    ProductoEvento estadoActual
) {}
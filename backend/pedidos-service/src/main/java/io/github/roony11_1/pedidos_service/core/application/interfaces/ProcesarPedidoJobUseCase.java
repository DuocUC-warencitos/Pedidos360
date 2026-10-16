package io.github.roony11_1.pedidos_service.core.application.interfaces;

import java.util.UUID;

public interface ProcesarPedidoJobUseCase
{
    /**
     * Procesa la reserva de stock para el job.
     * <p>
     * Negocio (409, NotFound, validación) -> marca FAILED y retorna normal (ACK).
     * Transitorio (5xx, timeout, DB) -> lanza excepción (NACK -> DLQ).
     */
    void procesar(UUID jobId);
}

package io.github.roony11_1.pedidos_service.api.dto.response;

import java.time.Instant;
import java.util.UUID;

import io.github.roony11_1.pedidos_service.core.domain.model.PedidoJob;

public record PedidoJobStatusResponse(
        UUID jobId,
        UUID pedidoId,
        String estadoJob,
        String estadoPedido,
        String error,
        Instant startedAt,
        Instant finishedAt)
{
    public static PedidoJobStatusResponse from(PedidoJob job, String estadoPedido)
    {
        return new PedidoJobStatusResponse(
                job.getId(),
                job.getPedidoId(),
                job.getEstado().name(),
                estadoPedido,
                job.getError(),
                job.getStartedAt(),
                job.getFinishedAt());
    }
}
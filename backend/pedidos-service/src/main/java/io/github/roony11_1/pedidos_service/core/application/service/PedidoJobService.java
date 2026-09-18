package io.github.roony11_1.pedidos_service.core.application.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.roony11_1.pedidos_service.core.domain.model.EstadoPedido;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoJob;
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoJobRepository;
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoRepository;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoClient;
import io.github.roony11_1.pedidos_service.kernel.IUserTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service 
@RequiredArgsConstructor 
public class PedidoJobService 
{
    private final PedidoJobRepository pedidoJobRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoClient productoClient;
    private final IUserTokenService userTokenService;
    private final ApplicationContext ctx;

    public PedidoJob crearEjecutar(UUID pedidodId, String idempotencyKey, String correlationId)
    {
        var ejecutadoPorId = userTokenService.getUserId();

        PedidoJob job = PedidoJob.builder()
            .pedidoId(pedidodId)
            .estado(PedidoJob.EstadoJob.RUNNING)
            .startedAt(Instant.now())
            .ejecutadoPorUserId(ejecutadoPorId)
            .correlationId(correlationId)
            .idempotencyKey(idempotencyKey)
            .build();

            job = pedidoJobRepository.save(job);

            PedidoJobService self = ctx.getBean(PedidoJobService.class);

            self.ejecutarAsync(job.getId());

            return job;
    }

    @Async("pedidoExecutor")
    public void ejecutarAsync(UUID jobId)
    {
        PedidoJob job = pedidoJobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalStateException("No hay un job con id: " + jobId.toString()));

        try
        {
            Pedido pedido = pedidoRepository.findByIdWithProductos(job.getPedidoId())
                    .orElseThrow();

            productoClient.reservar(job.getIdempotencyKey(), new ProductoClient.ReservaStockRequest(
                    pedido.getId(),
                    pedido.getProductos().stream()
                            .map(pp -> new ProductoClient.ReservaStockRequest.Item(
                                    pp.getProductoId(), pp.getCantidad()))
                            .toList()));

            // TX corta: job COMPLETED + pedido STOCK_RESERVADO
            ctx.getBean(PedidoJobService.class).marcarCompletado(job.getId());

        }
        catch (Exception ex)
        {
            log.error("Job {} falló para pedido {}", jobId, job.getPedidoId(), ex);
            ctx.getBean(PedidoJobService.class).marcarFallido(job.getId(), ex.getMessage());
        }
    }

    @Transactional (propagation = Propagation.REQUIRES_NEW)
    public void marcarCompletado(UUID jobId)
    {
        PedidoJob job = pedidoJobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("No hay un Job con id: " + jobId.toString()));

        job.marcarCompletado();

        Pedido pedido = pedidoRepository.findById(job.getPedidoId())
            .orElseThrow(() -> new IllegalArgumentException("No hay un pedido con id: " + job.getPedidoId().toString()));

        pedido.cambiarEstado(EstadoPedido.STOCK_RESERVADO);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarFallido(UUID jobId, String error)
    {
        PedidoJob job = pedidoJobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("No hay un Job con id: " + jobId.toString()));

        job.marcarFallido(error);

        Pedido pedido = pedidoRepository.findById(job.getPedidoId())
            .orElseThrow(() -> new IllegalArgumentException("No hay un pedido con id: " + job.getPedidoId().toString()));

        pedido.cambiarEstado(EstadoPedido.STOCK_FALLIDO);
    }
}

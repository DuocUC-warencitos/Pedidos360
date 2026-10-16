package io.github.roony11_1.pedidos_service.core.application.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import feign.FeignException;
import io.github.roony11_1.error.core.exceptions.NotFoundException;
import io.github.roony11_1.pedidos_service.core.application.event.PedidoStockFallidoEvent;
import io.github.roony11_1.pedidos_service.core.application.event.PedidoStockReservadoEvent;
import io.github.roony11_1.pedidos_service.core.application.interfaces.PedidoEventPublisher;
import io.github.roony11_1.pedidos_service.core.application.interfaces.ProcesarPedidoJobUseCase;
import io.github.roony11_1.pedidos_service.core.domain.model.EstadoPedido;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoJob;
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoJobRepository;
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoRepository;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoClient;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoServiceUnavailableException;
import io.github.roony11_1.pedidos_service.infrastructure.client.ReservaStockRechazadaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoJobService implements ProcesarPedidoJobUseCase
{
    private final PedidoJobRepository pedidoJobRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoClient productoClient;
    private final PedidoEventPublisher eventPublisher;
    private final ApplicationContext ctx;

    private PedidoJobService self()
    {
        return ctx.getBean(PedidoJobService.class);
    }

    @Override
    public void procesar(UUID jobId)
    {
        PedidoJob job = obtenerJob(jobId)
            .orElseThrow(() -> new NotFoundException("PedidoJob", jobId));

        if (job.getEstado() != PedidoJob.EstadoJob.RUNNING)
        {
            log.info("Job {} ya {} -> idempotente, skip", jobId, job.getEstado());
            return;
        }

        String userId = job.getEjecutadoPorUserId();

        try
        {
            Pedido pedido = pedidoRepository.findById(job.getPedidoId())
                .orElseThrow(() -> new NotFoundException("Pedido", job.getPedidoId()));

            if (userId == null)
                userId = pedido.getUserId();

            productoClient.reservar(job.getIdempotencyKey(), new ProductoClient.ReservaStockRequest(
                pedido.getId(),
                pedido.getProductos().stream()
                    .map(pp -> new ProductoClient.ReservaStockRequest.Item(
                        pp.getProductoId(), pp.getCantidad()))
                    .toList()));

            self().marcarCompletado(job.getId());
            publicarReservado(job, userId);
        }
        catch (NotFoundException | IllegalArgumentException ex)
        {
            self().marcarFallido(job.getId(), ex.getMessage());
            publicarFallido(job, userId, ex.getMessage());
        }
        catch (ReservaStockRechazadaException ex)
        {
            self().marcarFallido(job.getId(), ex.getMessage());
            publicarFallido(job, userId, ex.getMessage());
        }
        catch (FeignException ex)
        {
            if (ex.status() >= 400 && ex.status() < 500)
            {
                String limpio = ReservaStockRechazadaException.fromFeign(ex).getMessage();
                self().marcarFallido(job.getId(), limpio);
                publicarFallido(job, userId, limpio);
            }
            else
            {
                throw ex;
            }
        }
        catch (ProductoServiceUnavailableException ex)
        {
            String limpio = ReservaStockRechazadaException.mensajeLimpioDeCausa(ex);
            if (limpio != null)
            {
                self().marcarFallido(job.getId(), limpio);
                publicarFallido(job, userId, limpio);
            }
            else
            {
                throw ex;
            }
        }
    }

    private void publicarReservado(PedidoJob job, String userId)
    {
        try
        {
            eventPublisher.publicarStockReservado(new PedidoStockReservadoEvent(
                job.getId(), job.getPedidoId(), userId, job.getCorrelationId(), Instant.now()));
        }
        catch (Exception ex)
        {
            log.warn("Publish stock.reservado falló jobId={}", job.getId(), ex);
        }
    }

    private void publicarFallido(PedidoJob job, String userId, String error)
    {
        try
        {
            eventPublisher.publicarStockFallido(new PedidoStockFallidoEvent(
                job.getId(), job.getPedidoId(), userId, job.getCorrelationId(), error, Instant.now()));
        }
        catch (Exception ex)
        {
            log.warn("Publish stock.fallido falló jobId={}", job.getId(), ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarCompletado(UUID jobId)
    {
        PedidoJob job = obtenerJob(jobId)
            .orElseThrow(() -> new NotFoundException("PedidoJob", jobId));

        job.marcarCompletado();

        Pedido pedido = pedidoRepository.findById(job.getPedidoId())
            .orElseThrow(() -> new NotFoundException("Pedido", job.getPedidoId()));

        if (pedido.getEstadoPedido() == EstadoPedido.CREADO)
            pedido.cambiarEstado(EstadoPedido.STOCK_RESERVADO);

        if (pedido.getEstadoPedido() == EstadoPedido.STOCK_RESERVADO)
        {
            pedido.cambiarEstado(EstadoPedido.ACEPTADO);
            pedido.setComentario((pedido.getComentario() != null ? pedido.getComentario() + "\n" : "") + "Aceptado automáticamente tras reserva de stock");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarFallido(UUID jobId, String error)
    {
        PedidoJob job = obtenerJob(jobId)
            .orElseThrow(() -> new NotFoundException("PedidoJob", jobId));

        job.marcarFallido(error);

        Pedido pedido = pedidoRepository.findById(job.getPedidoId())
            .orElseThrow(() -> new NotFoundException("Pedido", job.getPedidoId()));

        pedido.cambiarEstado(EstadoPedido.STOCK_FALLIDO);
    }

    private Optional<PedidoJob> obtenerJob(UUID jobId)
    {
        return pedidoJobRepository.findById(jobId);
    }
}

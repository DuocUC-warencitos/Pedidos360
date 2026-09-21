package io.github.roony11_1.pedidos_service.core.application.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.roony11_1.error.core.exceptions.NotFoundException;
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
        // Idempotencia: si ya existe job para este key, retorna existente sin crear duplicado
        var existente = pedidoJobRepository.findByIdempotencyKey(idempotencyKey);
        if (existente.isPresent()) {
            log.info("Idempotency hit crearEjecutar key={} -> jobId={}", idempotencyKey, existente.get().getId());
            return existente.get();
        }

        var ejecutadoPorId = userTokenService.getUserId();

        PedidoJob job = PedidoJob.builder()
            .pedidoId(pedidodId)
            .estado(PedidoJob.EstadoJob.RUNNING)
            .startedAt(Instant.now())
            .ejecutadoPorUserId(ejecutadoPorId)
            .correlationId(correlationId)
            .idempotencyKey(idempotencyKey)
            .build();

        try {
            job = pedidoJobRepository.save(job);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.warn("DataIntegrityViolation crearEjecutar key={} -> recuperando existente", idempotencyKey, ex);
            return pedidoJobRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new NotFoundException("PedidoJob por IdempotencyKey", idempotencyKey));
        }

        PedidoJobService self = ctx.getBean(PedidoJobService.class);

        self.ejecutarAsync(job.getId());

        return job;
    }

    @Async("pedidoExecutor")
    public void ejecutarAsync(UUID jobId)
    {
        PedidoJob job = pedidoJobRepository.findById(jobId)
            .orElseThrow(() -> new NotFoundException("PedidoJob", jobId));

        try
        {
            Pedido pedido = pedidoRepository.findByIdWithProductos(job.getPedidoId())
                    .orElseThrow(() -> new NotFoundException("Pedido", job.getPedidoId()));

            productoClient.reservar(job.getIdempotencyKey(), new ProductoClient.ReservaStockRequest(
                    pedido.getId(),
                    pedido.getProductos().stream()
                            .map(pp -> new ProductoClient.ReservaStockRequest.Item(
                                    pp.getProductoId(), pp.getCantidad()))
                            .toList()));

            // TX corta: job COMPLETED + pedido ACEPTADO automático (STOCK_RESERVADO intermedio para trazabilidad)
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
            .orElseThrow(() -> new NotFoundException("PedidoJob", jobId));

        job.marcarCompletado();

        Pedido pedido = pedidoRepository.findById(job.getPedidoId())
            .orElseThrow(() -> new NotFoundException("Pedido", job.getPedidoId()));

        // EP1: aparece aceptado automático. Hace STOCK_RESERVADO -> ACEPTADO en misma TX para respetar flujo CREADO->STOCK_RESERVADO->ACEPTADO
        if (pedido.getEstadoPedido() == EstadoPedido.CREADO) {
            pedido.cambiarEstado(EstadoPedido.STOCK_RESERVADO);
        }
        if (pedido.getEstadoPedido() == EstadoPedido.STOCK_RESERVADO) {
            pedido.cambiarEstado(EstadoPedido.ACEPTADO);
            // comentario de auditoría opcional para trazabilidad
            pedido.setComentario((pedido.getComentario() != null ? pedido.getComentario() + "\n" : "") + "Aceptado automáticamente tras reserva de stock");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarFallido(UUID jobId, String error)
    {
        PedidoJob job = pedidoJobRepository.findById(jobId)
            .orElseThrow(() -> new NotFoundException("PedidoJob", jobId));

        job.marcarFallido(error);

        Pedido pedido = pedidoRepository.findById(job.getPedidoId())
            .orElseThrow(() -> new NotFoundException("Pedido", job.getPedidoId()));

        pedido.cambiarEstado(EstadoPedido.STOCK_FALLIDO);
    }
}

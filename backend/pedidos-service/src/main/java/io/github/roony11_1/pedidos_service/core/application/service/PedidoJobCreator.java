package io.github.roony11_1.pedidos_service.core.application.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import io.github.roony11_1.error.core.exceptions.NotFoundException;
import io.github.roony11_1.pedidos_service.core.application.event.PedidoCreadoEvent;
import io.github.roony11_1.pedidos_service.core.application.interfaces.PedidoEventPublisher;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoJob;
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoJobRepository;
import io.github.roony11_1.pedidos_service.kernel.IUserTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoJobCreator
{
    private final PedidoJobRepository pedidoJobRepository;
    private final IUserTokenService userTokenService;
    private final PedidoEventPublisher eventPublisher;
    private final ApplicationContext ctx;

    public PedidoJob crearEjecutar(UUID pedidoId, String idempotencyKey, String correlationId)
    {
        String jobKey = idempotencyKey + ":job";

        var existente = pedidoJobRepository
            .findByIdempotencyKey(jobKey);

        if (existente.isEmpty())
            existente = pedidoJobRepository.findByIdempotencyKey(idempotencyKey);

        if (existente.isPresent())
        {
            log.info("Idempotency hit crearEjecutar key={} jobKey={} -> jobId={}", idempotencyKey, jobKey, existente.get().getId());
            return existente.get();
        }

        var ejecutadoPorId = userTokenService.getUserId();

        PedidoJob job = PedidoJob.builder()
            .pedidoId(pedidoId)
            .estado(PedidoJob.EstadoJob.RUNNING)
            .startedAt(Instant.now())
            .ejecutadoPorUserId(ejecutadoPorId)
            .correlationId(correlationId)
            .idempotencyKey(jobKey)
            .build();

        final PedidoJob guardado;
        try
        {
            guardado = pedidoJobRepository.save(job);
        }
        catch (DataIntegrityViolationException ex)
        {
            log.warn("DataIntegrityViolation crearEjecutar key={} jobKey={} -> recuperando existente", idempotencyKey, jobKey, ex);

            return pedidoJobRepository.findByIdempotencyKey(jobKey)
                .or(() -> pedidoJobRepository.findByIdempotencyKey(idempotencyKey))
                .orElseThrow(() -> new NotFoundException("PedidoJob por IdempotencyKey", jobKey));
        }

        try
        {
            eventPublisher.publicarCreado(new PedidoCreadoEvent(
                guardado.getId(), pedidoId, ejecutadoPorId, jobKey, correlationId, Instant.now()));
        }
        catch (Exception ex)
        {
            // Broker caído: se deja el job FAILED + pedido STOCK_FALLIDO (mismo
            // estado terminal que un fallo async) y se responde 202 con el job
            // para que el frontend lo vea por polling en vez de un 500 sin jobId.
            log.error("Publish pedido.crear falló jobId={} -> FAILED", guardado.getId(), ex);
            ctx.getBean(PedidoJobService.class).marcarFallido(
                guardado.getId(), "No se pudo encolar el pedido para procesamiento, reintente");

            return pedidoJobRepository.findById(guardado.getId())
                .orElseThrow(() -> new NotFoundException("PedidoJob", guardado.getId()));
        }

        return guardado;
    }
}

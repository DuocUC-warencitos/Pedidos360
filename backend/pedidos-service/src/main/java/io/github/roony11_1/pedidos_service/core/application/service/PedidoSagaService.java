package io.github.roony11_1.pedidos_service.core.application.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.roony11_1.error.core.exceptions.NotFoundException;
import io.github.roony11_1.pedidos_service.api.dto.response.PedidoJobStatusResponse;
import io.github.roony11_1.pedidos_service.core.domain.model.EstadoPedido;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoJob;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoJobRepository;
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoRepository;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoClient;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoClientResiliente;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoServiceUnavailableException;
import io.github.roony11_1.pedidos_service.infrastructure.mensajeria.PedidoEventoProducer;
import io.github.roony11_1.pedidos_service.kernel.IUserTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service 
@RequiredArgsConstructor 
public class PedidoSagaService 
{
    private final PedidoRepository pedidoRepository;
    private final PedidoJobRepository pedidoJobRepository;
    private final IUserTokenService userTokenService;
    private final TransactionTemplate txTemplate;
    private final ProductoClientResiliente productoClientResiliente;
    private final PedidoEventoProducer eventoProducer;

    public Pedido crearPedido(List<PedidoProducto> productos, String idempotencyKey, String correlationId)
    {
        Optional<Pedido> pedidoOpt = pedidoRepository.findByIdempotencyKey(idempotencyKey);

        if (pedidoOpt.isPresent()) 
        {
            Pedido pedido = pedidoOpt.get();

            log.info("Idempotency hit crearPedido key={} -> pedidoId={}", idempotencyKey, pedido.getId());

            return pedido;
        }

        eventoProducer.publicar();

        try 
        {
            return txTemplate.execute(status ->
            {
                var existenteTx = pedidoRepository.findByIdempotencyKey(idempotencyKey);

                if (existenteTx.isPresent()) 
                    return existenteTx.get();

                var pedido = new Pedido();
                productos.forEach(pedido::addProducto);
                pedido.setUserId(userTokenService.getUserId());
                pedido.setEstadoPedido(EstadoPedido.CREADO);
                pedido.setComentario(userTokenService.getAuditComentario("Ingresado por"));
                pedido.setIdempotencyKey(idempotencyKey);
                pedido.setCorrelationId(correlationId);

                return pedidoRepository.save(pedido);
            });
        } 
        catch (DataIntegrityViolationException ex) 
        {
            log.warn("DataIntegrityViolation por idempotencyKey={} -> recuperando existente", idempotencyKey, ex);
            return pedidoRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new NotFoundException("Pedido por IdempotencyKey", idempotencyKey));
        }
    }

    public void cancelarConCompensacion(UUID pedidoId, String motivo)
    {
        var pedido = pedidoRepository.findById(pedidoId)
                            .orElseThrow(() -> new NotFoundException("Pedido", pedidoId));

        var estadosConReserva = Set.of(EstadoPedido.STOCK_RESERVADO, EstadoPedido.ACEPTADO, EstadoPedido.CONFIRMADO, EstadoPedido.EN_PREPARACION, EstadoPedido.DESPACHADO);

        if (estadosConReserva.contains(pedido.getEstadoPedido()))
        {
            var liberarReq = new ProductoClient.LiberarStockRequest(
                pedido.getId(),
                pedido.getProductos().stream()
                    .map(pp -> new ProductoClient.LiberarStockRequest.Item(pp.getProductoId(), pp.getCantidad()))
                    .toList()
            );
            
            try
            {
                productoClientResiliente.liberar(liberarReq);
                log.info("Stock liberado para pedidoId={} estado={} items={}", pedidoId, pedido.getEstadoPedido(), liberarReq.items().size());
            }
            catch (Exception ex)
            {
                log.error("Fallo liberando stock pedido {} estado {}: {}", pedidoId, pedido.getEstadoPedido(), ex.getMessage(), ex);

                throw new ProductoServiceUnavailableException("No se pudo liberar stock para cancelar pedido " + pedidoId + ": " + ex.getMessage(), ex);
            }
        }

        txTemplate.executeWithoutResult(status ->
        {
            // Recarga dentro de TX para asegurar versión fresca y dirty-check
            var pedidoTx = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new NotFoundException("Pedido", pedidoId));
                
            pedidoTx.cancelar(userTokenService.getAuditComentario("Cancelado por: " + motivo));
        });
    }

    public PedidoJobStatusResponse estado(UUID jobId)
    {
        PedidoJob job = pedidoJobRepository.findById(jobId)
            .orElseThrow(() -> new NotFoundException("PedidoJob", jobId));

        String estadoPedido = pedidoRepository.findById(job.getPedidoId())
            .map(p -> p.getEstadoPedido().name())
            .orElse("DESCONOCIDO");

        return PedidoJobStatusResponse.from(job, estadoPedido);
    }
}

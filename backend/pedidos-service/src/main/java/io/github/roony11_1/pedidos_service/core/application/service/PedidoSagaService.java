package io.github.roony11_1.pedidos_service.core.application.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.roony11_1.pedidos_service.core.domain.model.EstadoPedido;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoRepository;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoClient;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoClientResiliente;
import io.github.roony11_1.pedidos_service.kernel.IUserTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Service 
@RequiredArgsConstructor 
public class PedidoSagaService 
{
    private final PedidoRepository pedidoRepository;
    private final IUserTokenService userTokenService;
    private final TransactionTemplate txTemplate;
    private final ProductoClientResiliente productoClientResiliente;

    public Pedido crearPedido(List<PedidoProducto> productos, String idempotencyKey, String correlationId)
    {
        // Fast-path: idempotencia sin crear duplicado
        var existenteOpt = pedidoRepository.findByIdempotencyKey(idempotencyKey);
        if (existenteOpt.isPresent()) {
            log.info("Idempotency hit crearPedido key={} -> pedidoId={}", idempotencyKey, existenteOpt.get().getId());
            return existenteOpt.get();
        }

        try {
            return txTemplate.execute(status ->
            {
                // Re-check dentro de TX por carrera
                var existenteTx = pedidoRepository.findByIdempotencyKey(idempotencyKey);
                if (existenteTx.isPresent()) return existenteTx.get();

                var pedido = new Pedido();
                productos.forEach(pedido::addProducto);
                pedido.setUserId(userTokenService.getUserId());
                pedido.setEstadoPedido(EstadoPedido.CREADO);
                pedido.setComentario(userTokenService.getAuditComentario("Ingresado por"));
                pedido.setIdempotencyKey(idempotencyKey);
                pedido.setCorrelationId(correlationId);

                return pedidoRepository.save(pedido);
            });
        } catch (DataIntegrityViolationException ex) {
            // Carrera: otro thread insertó misma key entre check y save (unique constraint)
            log.warn("DataIntegrityViolation por idempotencyKey={} -> recuperando existente", idempotencyKey, ex);
            return pedidoRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> ex);
        }
    }

    public void cancelarConCompensacion(UUID pedidoId, String motivo)
    {
        var pedido = pedidoRepository.findByIdWithProductos(pedidoId)
                            .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + pedidoId));

        // Siempre intenta liberar si pudo haber reserva: idempotente en producto-service (no-op si no existe)
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
                // No se cancela localmente si la compensación falla: deja en estado actual para retry manual
                // Lanza para que el controller mapee a 503 / 409 y el frontend pueda reintentar
                throw new IllegalStateException("No se pudo liberar stock para cancelar pedido " + pedidoId + ": " + ex.getMessage(), ex);
            }
        }

        txTemplate.executeWithoutResult(status -> 
        {
            // Recarga dentro de TX para asegurar versión fresca y dirty-check
            var pedidoTx = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + pedidoId));
            pedidoTx.cancelar(userTokenService.getAuditComentario("Cancelado por: " + motivo));
        });
    }
}

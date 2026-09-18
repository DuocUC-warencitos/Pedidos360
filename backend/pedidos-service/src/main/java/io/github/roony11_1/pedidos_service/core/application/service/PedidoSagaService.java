package io.github.roony11_1.pedidos_service.core.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.roony11_1.pedidos_service.core.domain.model.EstadoPedido;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoRepository;
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

    public Pedido crearPedido(List<PedidoProducto> productos, String idempotencyKey, String correlationId)
    {
        return txTemplate.execute(status ->
        {
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

    public void cancelarConCompensacion(UUID pedidoId, String motivo)
    {
        var pedido = pedidoRepository.findByIdWithProductos(pedidoId)
                            .orElseThrow();

        if (pedido.getEstadoPedido() == EstadoPedido.STOCK_RESERVADO)
        {
            try
            {
                // productoClient.liberar(new LiberarStockRequest(pedido.getId()));
            }
            catch (Exception ex)
            {
                log.error("Fallo liberando stock pedido {}: {}", pedidoId, ex.getMessage(), ex);
                // TODO: outbox / reintento asíncrono
            }
        }

        txTemplate.executeWithoutResult(status -> 
        {
            pedido.cancelar(userTokenService.getAuditComentario("Cancelado por: " + motivo));
        });
    }
}

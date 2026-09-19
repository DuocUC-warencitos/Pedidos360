package io.github.roony11_1.pedidos_service.core.application.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class PedidoService 
{
    private final PedidoRepository pedidoRepository;
    private final IUserTokenService userTokenService;
    private final ProductoClientResiliente productoClientResiliente;

    @Transactional
    public Pedido save(List<PedidoProducto> productos) 
    {

        var pedido = new Pedido();

        productos.forEach(pedido::addProducto);

        pedido.setUserId(userTokenService.getUserId());
        pedido.setEstadoPedido(EstadoPedido.CREADO);
        pedido.setComentario(userTokenService.getAuditComentario("Ingresado por"));

        var saved = pedidoRepository.save(pedido);

        var pedidoCompleto = pedidoRepository.findByIdWithProductos(saved.getId())
            .orElseThrow();

        return pedidoCompleto;
    }

    @Transactional(readOnly = true)
    public List<Pedido> findAll()
    {
        return pedidoRepository.findAllWithProductos();
    }

    @Transactional
    public void deleteAll()
    {
        var pedidos = pedidoRepository.findAllWithProductos();
        var estadosConReserva = Set.of(EstadoPedido.STOCK_RESERVADO, EstadoPedido.ACEPTADO, EstadoPedido.CONFIRMADO, EstadoPedido.EN_PREPARACION, EstadoPedido.DESPACHADO);
        for (var pedido : pedidos) {
            if (estadosConReserva.contains(pedido.getEstadoPedido()) && !pedido.getProductos().isEmpty()) {
                var liberarReq = new ProductoClient.LiberarStockRequest(
                    pedido.getId(),
                    pedido.getProductos().stream()
                        .map(pp -> new ProductoClient.LiberarStockRequest.Item(pp.getProductoId(), pp.getCantidad()))
                        .toList()
                );
                try {
                    productoClientResiliente.liberar(liberarReq);
                    log.info("Stock liberado deleteAll pedidoId={} estado={} items={}", pedido.getId(), pedido.getEstadoPedido(), liberarReq.items().size());
                } catch (Exception ex) {
                    log.warn("Fallo liberando stock deleteAll pedido {} estado {}: {}", pedido.getId(), pedido.getEstadoPedido(), ex.getMessage());
                    // continúa borrando aunque falle un pedido (idempotente, puede reintentar después)
                }
            }
        }
        pedidoRepository.deleteAllProductos();
        pedidoRepository.deleteAllPedidos();
    }

    @Transactional
    public void avanzarEstado(UUID id)
    {
        var pedido = pedidoRepository.findById(id)
            .orElseThrow();

        EstadoPedido siguiente = pedido.getEstadoPedido().siguiente()
            .orElseThrow(() -> new IllegalStateException("El pedido ya está en un estado final: " + pedido.getEstadoPedido()));

        String comentario = userTokenService.getAuditComentario("Estado actualizado a " + siguiente + " por");
        pedido.avanzarEstado(comentario);
    }

    @Transactional
    public void cancelar(UUID id)
    {
        var pedido = pedidoRepository.findById(id)
            .orElseThrow();

        String comentario = userTokenService.getAuditComentario("Cancelado por");
        pedido.cancelar(comentario);
    }
}

package io.github.roony11_1.pedidos_service.core.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.roony11_1.pedidos_service.core.domain.model.EstadoPedido;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoRepository;
import io.github.roony11_1.pedidos_service.kernel.IUserTokenService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService 
{
    private final PedidoRepository pedidoRepository;
    private final IUserTokenService userTokenService;

    @Transactional
    public Pedido save(List<PedidoProducto> productos) 
    {

        var pedido = new Pedido();

        productos.forEach(pedido::addProducto);

        pedido.setUserId(userTokenService.getUserId());
        pedido.setEstadoPedido(EstadoPedido.CREADO);

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
        pedidoRepository.deleteAllProductos();
        pedidoRepository.deleteAllPedidos();
    }

    @Transactional
    public void avanzarEstado(UUID id)
    {
        var pedido = pedidoRepository.findById(id)
            .orElseThrow();

        pedido.avanzarEstado();
    }

    @Transactional
    public void cancelar(UUID id)
    {
        var pedido = pedidoRepository.findById(id)
            .orElseThrow();

        pedido.cancelar();
    }
}

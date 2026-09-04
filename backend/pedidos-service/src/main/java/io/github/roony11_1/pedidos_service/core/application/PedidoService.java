package io.github.roony11_1.pedidos_service.core.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.roony11_1.pedidos_service.core.domain.model.EstadoPedido;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService 
{
    private final PedidoRepository pedidoRepository;

    @Transactional
    public Pedido save(List<PedidoProducto> productos)
    {
        var pedido = new Pedido();

        productos.forEach(pedido::addProducto);

        pedido.setEstadoPedido(EstadoPedido.CREADO);

        return pedidoRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public List<Pedido> findAll()
    {
        return pedidoRepository.findAllWithProductos();
    }
}

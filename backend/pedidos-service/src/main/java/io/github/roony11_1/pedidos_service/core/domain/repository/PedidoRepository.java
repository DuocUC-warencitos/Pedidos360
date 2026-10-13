package io.github.roony11_1.pedidos_service.core.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, UUID>, JpaSpecificationExecutor<Pedido>
{
    Optional<Pedido> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estadoPedido = io.github.roony11_1.pedidos_service.core.domain.model.EstadoPedido.CANCELADO")
    long contarCancelados();

    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estadoPedido NOT IN (io.github.roony11_1.pedidos_service.core.domain.model.EstadoPedido.ENTREGADO, io.github.roony11_1.pedidos_service.core.domain.model.EstadoPedido.CANCELADO, io.github.roony11_1.pedidos_service.core.domain.model.EstadoPedido.STOCK_FALLIDO)")
    long contarCancelables();
}

package io.github.roony11_1.pedidos_service.core.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long>
{
    @Query("""
        SELECT DISTINCT p
        FROM Pedido p
        LEFT JOIN FETCH p.productos
    """)
    List<Pedido> findAllWithProductos();
}

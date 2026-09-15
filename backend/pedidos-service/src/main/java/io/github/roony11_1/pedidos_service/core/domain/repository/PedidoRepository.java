package io.github.roony11_1.pedidos_service.core.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import jakarta.transaction.Transactional;

public interface PedidoRepository extends JpaRepository<Pedido, Long>
{
    @Query("""
        SELECT DISTINCT p
        FROM Pedido p
        LEFT JOIN FETCH p.productos
    """)
    List<Pedido> findAllWithProductos();

    @Query("""
        SELECT DISTINCT p
        FROM Pedido p
        LEFT JOIN FETCH p.productos
        WHERE p.id = :id
    """)
    Optional<Pedido> findByIdWithProductos(Long id);

    @Modifying
    @Transactional
    @Query("DELETE FROM PedidoProducto pp")
    void deleteAllProductos();

    @Modifying
    @Transactional
    @Query("DELETE FROM Pedido p")
    void deleteAllPedidos();
}

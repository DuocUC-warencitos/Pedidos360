package io.github.roony11_1.producto_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.roony11_1.producto_service.model.ReservaStock;

public interface ReservaStockRepository extends JpaRepository<ReservaStock, UUID>
{
    Optional<ReservaStock> findByIdempotencyKey(String idempotencyKey);
    boolean existsByIdempotencyKey(String idempotencyKey);
    Optional<ReservaStock> findByPedidoId(UUID pedidoId);
}

package io.github.roony11_1.pedidos_service.core.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, UUID>, JpaSpecificationExecutor<Pedido>
{
    Optional<Pedido> findByIdempotencyKey(String idempotencyKey);
}

package io.github.roony11_1.pedidos_service.core.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.roony11_1.pedidos_service.core.domain.model.PedidoJob;

public interface PedidoJobRepository extends JpaRepository<PedidoJob, UUID>
{
    List<PedidoJob> findByEstado(PedidoJob.EstadoJob estado);
    List<PedidoJob> findByPedidoId(UUID pedidoId);
    Optional<PedidoJob> findByIdempotencyKey(String idempotencyKey);
}

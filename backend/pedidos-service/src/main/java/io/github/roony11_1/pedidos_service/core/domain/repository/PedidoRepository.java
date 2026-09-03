package io.github.roony11_1.pedidos_service.core.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long>
{

}

package io.github.roony11_1.producto_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.roony11_1.producto_service.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, UUID>
{

}

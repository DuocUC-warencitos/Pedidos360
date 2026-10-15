package io.github.roony11_1.producto_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import io.github.roony11_1.producto_service.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, UUID>, JpaSpecificationExecutor<Producto>
{
    @Query("SELECT COUNT(p) FROM Producto p WHERE p.activo = true")
    long contarActivos();

    @Query("SELECT COALESCE(SUM(p.stockDisponible), 0) FROM Producto p")
    long sumarStockDisponible();

    @Query("SELECT COALESCE(SUM(p.stockReservado), 0) FROM Producto p")
    long sumarStockReservado();

    @Query("SELECT DISTINCT p.categoria FROM Producto p WHERE p.categoria IS NOT NULL ORDER BY p.categoria ASC")
    List<String> listarCategorias();
}

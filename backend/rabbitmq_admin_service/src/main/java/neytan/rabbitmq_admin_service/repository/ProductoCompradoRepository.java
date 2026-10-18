package neytan.rabbitmq_admin_service.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import neytan.rabbitmq_admin_service.model.ProductoComprado;

public interface ProductoCompradoRepository
        extends JpaRepository<ProductoComprado, Long> {

    @Query("SELECT p.productoId, SUM(p.cantidad) FROM ProductoComprado p "
            + "WHERE p.fecha BETWEEN :desde AND :hasta "
            + "GROUP BY p.productoId ORDER BY SUM(p.cantidad) DESC")
    List<Object[]> rankingPorPeriodo(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            Pageable pageable);
}

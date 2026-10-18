package neytan.rabbitmq_admin_service.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import neytan.rabbitmq_admin_service.model.MetricaDiaria;

public interface MetricaDiariaRepository
        extends JpaRepository<MetricaDiaria, Long> {

    Optional<MetricaDiaria> findByRoutingKeyAndFecha(String routingKey, LocalDate fecha);

    List<MetricaDiaria> findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate desde);
}

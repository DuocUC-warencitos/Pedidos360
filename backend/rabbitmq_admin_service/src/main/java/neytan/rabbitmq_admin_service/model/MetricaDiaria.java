package neytan.rabbitmq_admin_service.model;

import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name = "metrica_diaria", uniqueConstraints = @UniqueConstraint(columnNames = { "routing_key", "fecha" }))
public class MetricaDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "routing_key", nullable = false)
    private String routingKey;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private long total;

    public MetricaDiaria() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}

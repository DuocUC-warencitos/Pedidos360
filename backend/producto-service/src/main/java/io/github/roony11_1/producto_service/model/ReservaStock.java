package io.github.roony11_1.producto_service.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reserva_stock", uniqueConstraints = @UniqueConstraint(name = "uk_reserva_idempotency", columnNames = "idempotency_key"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaStock 
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "pedido_id", nullable = false)
    private UUID pedidoId;

    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;

    @CreationTimestamp
    private Instant createdAt;
}

package io.github.roony11_1.pedidos_service.core.domain.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "pedido_job", 
    indexes = 
    { 
        @Index(name = "ix_pedido_job_estado", columnList = "estado"), 
        @Index(name = "ix_pedido_job_pedido", columnList = "pedido_id") 
    },
    uniqueConstraints = @jakarta.persistence.UniqueConstraint(name = "uk_pedido_job_idempotency", columnNames = "idempotency_key"))
@Data
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 
public class PedidoJob 
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pedido_id", nullable = false)
    private UUID pedidoId;

    @Enumerated(EnumType.STRING)
    private EstadoJob estado;
    
    private Instant startedAt;

    private Instant finishedAt;

    @Column(name = "ejecutado_por_userId", length = 100)
    private String ejecutadoPorUserId;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "error", length = 500)
    private String error;

    @Version
    private Long version;

    public enum EstadoJob { RUNNING, COMPLETED, FAILED }

    public void marcarCompletado()
    {
        estado = EstadoJob.COMPLETED;
        finishedAt = Instant.now();
    }

    public void marcarFallido(String error)
    {
        estado = EstadoJob.FAILED;
        finishedAt = Instant.now();
        this.error = truncar(error, 500);
    }

    private static String truncar(String s, int max)
    {
        if (s == null) 
            return null;
        
        return s.length() <= max ? s : s.substring(0, max);
    }
}

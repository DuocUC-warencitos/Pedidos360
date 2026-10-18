package neytan.rabbitmq_admin_service.model;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "eventos_auditoria")
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "routing_key", nullable = false)
    private String routingKey;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "pedido_id")
    private String pedidoId;

    @Column(name = "job_id")
    private String jobId;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "recibido_at", nullable = false)
    private Instant recibidoAt;

    public EventoAuditoria() {
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

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(String pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getRecibidoAt() {
        return recibidoAt;
    }

    public void setRecibidoAt(Instant recibidoAt) {
        this.recibidoAt = recibidoAt;
    }
}

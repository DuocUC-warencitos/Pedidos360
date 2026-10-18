package neytan.rabbitmq_admin_service.model;


import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;

@Entity
@Table(
    name = "notificaciones",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_notificaciones_evento_id",
        columnNames = "evento_id"
    )
)
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_usuario", nullable = false)
    private String idUsuario;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "evento_id", length = 255)
    private String eventoId;

    @CreationTimestamp
    @Column(name = "creada_en", updatable = false)
    private Instant creadaEn;

    public Notificacion() {
    }

    public Notificacion(String idUsuario, String message) {
        this.idUsuario = idUsuario;
        this.message = message;
    }

    public Notificacion(String idUsuario, String message, String eventoId) {
        this(idUsuario, message);
        this.eventoId = eventoId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEventoId() {
        return eventoId;
    }

    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }
}

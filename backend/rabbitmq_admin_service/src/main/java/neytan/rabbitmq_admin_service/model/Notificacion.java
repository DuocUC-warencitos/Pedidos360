package neytan.rabbitmq_admin_service.model;


import jakarta.persistence.*;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_usuario", nullable = false)
    private String idUsuario;

    @Column(nullable = false, length = 1000)
    private String message;

    public Notificacion() {
    }

    public Notificacion(String idUsuario, String message) {
        this.idUsuario = idUsuario;
        this.message = message;
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
}
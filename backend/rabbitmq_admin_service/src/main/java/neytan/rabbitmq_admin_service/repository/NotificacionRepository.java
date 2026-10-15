package neytan.rabbitmq_admin_service.repository;

import neytan.rabbitmq_admin_service.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository
        extends JpaRepository<Notificacion, Long> {

            List<Notificacion> findByIdUsuario(String idUsuario);
        Optional<Notificacion> findByIdAndIdUsuario(
            Long id,
            String idUsuario
    );
}

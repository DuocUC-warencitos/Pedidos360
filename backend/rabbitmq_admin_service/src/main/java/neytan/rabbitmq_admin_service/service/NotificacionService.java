package neytan.rabbitmq_admin_service.service;

import neytan.rabbitmq_admin_service.model.Notificacion;
import neytan.rabbitmq_admin_service.repository.NotificacionRepository;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import neytan.rabbitmq_admin_service.config.UnauthenticatedException;


import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@Service
public class NotificacionService
    {

    private final NotificacionRepository repository;

    public NotificacionService(
            NotificacionRepository repository) {
        this.repository = repository;
    }
    private String getUserId() {
    var authentication =
            SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
            || !(authentication.getPrincipal() instanceof Jwt jwt)) {
        throw new UnauthenticatedException(
                "No hay un usuario autenticado");
    }

    return jwt.getSubject();
}

    
    public Notificacion crear(Notificacion notificacion) {
        return repository.save(notificacion);
    }

    
    public List<Notificacion> listarTodas() {
        return repository.findAll();
    }

    public List<Notificacion> listarPorUsuario(int limit) {
        int limite = Math.min(Math.max(limit, 1), 50);
        return repository.findByIdUsuarioOrderByIdDesc(getUserId(), PageRequest.of(0, limite));
    }

   
    public Notificacion buscarPorId(Long id) {
        return repository.findByIdAndIdUsuario(id, getUserId())
                .orElseThrow(() ->
                    new RuntimeException(
                        "Notificación no encontrada: " + id));
    }

  
    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException(
                "Notificación no encontrada: " + id);
        }

        repository.deleteById(id);
    }
}
package neytan.rabbitmq_admin_service.controller;

import neytan.rabbitmq_admin_service.model.Notificacion;
import neytan.rabbitmq_admin_service.service.NotificacionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(
            NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }


    // Listar todas las notificaciones
    @GetMapping
    public ResponseEntity<List<Notificacion>> listarTodas() {

        List<Notificacion> notificaciones =
                notificacionService.listarTodas();

        return ResponseEntity.ok(notificaciones);
    }

    // Buscar notificaciones por usuario
    @GetMapping("/usuario/")
    public ResponseEntity<List<Notificacion>> listarPorUsuario(
            ) {

        List<Notificacion> notificaciones =
                notificacionService.listarPorUsuario();

        return ResponseEntity.ok(notificaciones);
    }

    // Buscar una notificación por ID
    @GetMapping("/{id}")
    public ResponseEntity<Notificacion> buscarPorId(
            @PathVariable Long id) {

        Notificacion notificacion =
                notificacionService.buscarPorId(id);

        return ResponseEntity.ok(notificacion);
    }

    // Eliminar una notificación
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id) {

        notificacionService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
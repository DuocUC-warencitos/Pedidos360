package neytan.rabbitmq_admin_service.controller;
import neytan.rabbitmq_admin_service.dto.ApiMessage;
import neytan.rabbitmq_admin_service.dto.CreateBindingRequest;
import neytan.rabbitmq_admin_service.dto.CreateExchangeRequest;
import neytan.rabbitmq_admin_service.dto.CreateQueueRequest;
import neytan.rabbitmq_admin_service.service.RabbitAdminService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController 
@RequestMapping ("/api/admin")
public class RabbitAdminController {
     private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping("/queues")
    public ResponseEntity<ApiMessage> crearCola(
            @Valid @RequestBody CreateQueueRequest request) {

        service.crearCola(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessage(
                        "Cola creada correctamente",
                        request.name()));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<ApiMessage> eliminarCola(
            @PathVariable String name) {

        validarNombre(name);
        service.eliminarCola(name);

        return ResponseEntity.ok(new ApiMessage(
                "Cola eliminada correctamente", name));
    }

    @PostMapping("/exchanges")
    public ResponseEntity<ApiMessage> crearExchange(
            @Valid @RequestBody CreateExchangeRequest request) {

        service.crearExchange(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessage(
                        "Exchange creado correctamente",
                        request.name()));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<ApiMessage> eliminarExchange(
            @PathVariable String name) {

        validarNombre(name);
        service.eliminarExchange(name);

        return ResponseEntity.ok(new ApiMessage(
                "Exchange eliminado correctamente", name));
    }

    @PostMapping("/bindings")
    public ResponseEntity<ApiMessage> crearBinding(
            @Valid @RequestBody CreateBindingRequest request) {

        service.crearBinding(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessage(
                        "Binding creado correctamente",
                        request.queueName() + " -> "
                                + request.exchangeName()));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<ApiMessage> eliminarBinding(
            @Valid @RequestBody CreateBindingRequest request) {

        service.eliminarBinding(request);

        return ResponseEntity.ok(new ApiMessage(
                "Binding eliminado correctamente",
                request.queueName() + " -> "
                        + request.exchangeName()));
    }

    private void validarNombre(String name) {
        if (name == null
                || !name.matches("^[a-zA-Z0-9._-]{1,100}$")) {
            throw new IllegalArgumentException(
                    "Nombre de recurso inválido");
        }
    }

}

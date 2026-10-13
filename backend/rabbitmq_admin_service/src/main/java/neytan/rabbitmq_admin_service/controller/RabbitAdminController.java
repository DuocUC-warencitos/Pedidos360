
package neytan.rabbitmq_admin_service.controller;

import jakarta.validation.Valid;

import neytan.rabbitmq_admin_service.dto.ApiMessage;
import neytan.rabbitmq_admin_service.dto.BindingInfo;
import neytan.rabbitmq_admin_service.dto.CreateBindingRequest;
import neytan.rabbitmq_admin_service.dto.CreateExchangeRequest;
import neytan.rabbitmq_admin_service.dto.CreateQueueRequest;
import neytan.rabbitmq_admin_service.dto.ExchangeInfo;
import neytan.rabbitmq_admin_service.dto.QueueInfo;
import neytan.rabbitmq_admin_service.service.RabbitAdminService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rabbit")
public class RabbitAdminController {

    private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @GetMapping("/queues")
    public List<QueueInfo> listarColas() {
        return service.listarColas();
    }

    @GetMapping("/exchanges")
    public List<ExchangeInfo> listarExchanges() {
        return service.listarExchanges();
    }

    @GetMapping("/bindings")
    public List<BindingInfo> listarBindings() {
        return service.listarBindings();
    }

    @PostMapping("/queues")
    public ResponseEntity<ApiMessage> crearCola(
            @Valid @RequestBody CreateQueueRequest request) {

        service.crearCola(request);

        String nombre = request.name().startsWith("pedidos360.")
                ? request.name()
                : "pedidos360." + request.name().trim() + ".queue";

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessage("Cola creada correctamente", nombre));
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<ApiMessage> eliminarCola(
            @PathVariable String name) {

        service.eliminarCola(name);

        return ResponseEntity.ok(
                new ApiMessage("Cola eliminada correctamente", name));
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

        service.eliminarExchange(name);

        return ResponseEntity.ok(
                new ApiMessage("Exchange eliminado correctamente", name));
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

        return ResponseEntity.ok(
                new ApiMessage(
                        "Binding eliminado correctamente",
                        request.queueName() + " -> "
                                + request.exchangeName()));
    }
}

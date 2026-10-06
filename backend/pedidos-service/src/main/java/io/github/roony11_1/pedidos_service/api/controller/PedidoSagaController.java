package io.github.roony11_1.pedidos_service.api.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.roony11_1.pedidos_service.api.dto.request.CrearPedidoRequest;
import io.github.roony11_1.pedidos_service.api.dto.response.PedidoJobStatusResponse;
import io.github.roony11_1.pedidos_service.core.application.service.PedidoJobService;
import io.github.roony11_1.pedidos_service.core.application.service.PedidoSagaService;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoJob;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pedidos/saga")
@RequiredArgsConstructor 
public class PedidoSagaController 
{
    private final PedidoSagaService pedidoSagaService;
    private final PedidoJobService pedidojobService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','CLIENTE')")
    public ResponseEntity<PedidoJobStatusResponse> crear(@RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody CrearPedidoRequest request)
    {
        String correlationId = UUID.randomUUID().toString();

        Pedido pedido = pedidoSagaService.crearPedido(request.toProductos(), idempotencyKey, correlationId);

        var pedidoId = pedido.getId();

        PedidoJob job = pedidojobService.crearEjecutar(pedidoId, idempotencyKey, correlationId);

        String estadoPedido = pedido.getEstadoPedido().name();

        return ResponseEntity.accepted()
            .header("Location", "/api/v1/pedidos/saga/jobs/" + job.getId())
            .body(PedidoJobStatusResponse.from(job, estadoPedido));
    }

    @PatchMapping("/{pedidoId}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR','CLIENTE')")
    public ResponseEntity<Void> cancelarConCompensacion(@PathVariable UUID pedidoId) 
    {
        pedidoSagaService.cancelarConCompensacion(pedidoId, "saga-cancel");

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<PedidoJobStatusResponse> estado(@PathVariable UUID jobId)
    {
        var response = pedidoSagaService.estado(jobId);

        return ResponseEntity.ok(response);
    }
}

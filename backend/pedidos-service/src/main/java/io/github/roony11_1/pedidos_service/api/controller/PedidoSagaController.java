package io.github.roony11_1.pedidos_service.api.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoJobRepository;
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pedidos/saga")
@RequiredArgsConstructor 
public class PedidoSagaController 
{
    private final PedidoSagaService pedidoSagaService;
    private final PedidoJobService pedidojobService;
    private final PedidoJobRepository pedidoJobRepository;
    private final PedidoRepository pedidoRepository;

    @PostMapping
    public ResponseEntity<PedidoJobStatusResponse> crear(@RequestHeader("Idempotency-Key") String idempotencyKey, @Valid @RequestBody CrearPedidoRequest request)
    {
        String correlationId = UUID.randomUUID().toString();

        Pedido pedido = pedidoSagaService.crearPedido(request.toProductos(), idempotencyKey, correlationId);

        var pedidoId = pedido.getId();

        PedidoJob job = pedidojobService.crearEjecutar(pedidoId, idempotencyKey, correlationId);

        return ResponseEntity.accepted()
            .header("Location", "api/v1/pedidos/saga/jobs/" + job.getId())
            .body(PedidoJobStatusResponse.from(job, "CREADO"));
    }

    @GetMapping("/jobs/{jobId}")
    public PedidoJobStatusResponse estado(@PathVariable UUID jobId)
    {
        PedidoJob job = pedidoJobRepository.findById(jobId)
            .orElseThrow();

        String estadoPedido = pedidoRepository.findById(job.getPedidoId())
            .map(p -> p.getEstadoPedido().name())
            .orElse("DESCONOCIDO");

        return PedidoJobStatusResponse.from(job, estadoPedido);
    }
}

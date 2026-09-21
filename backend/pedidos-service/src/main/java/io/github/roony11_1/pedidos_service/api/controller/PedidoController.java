package io.github.roony11_1.pedidos_service.api.controller;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.roony11_1.pedidos_service.api.dto.response.PedidoResponse;
import io.github.roony11_1.pedidos_service.core.application.service.PedidoSagaService;
import io.github.roony11_1.pedidos_service.core.application.service.PedidoService;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor 
public class PedidoController 
{
    private final PedidoService pedidoService;
    private final PedidoSagaService pedidoSagaService;
    private final Function<Pedido, PedidoResponse> pedidoResponseMapper;

    @GetMapping("/all")
    public ResponseEntity<List<PedidoResponse>> findAll()
    {
        var pedidos = pedidoService.findAll().stream()
            .map(pedidoResponseMapper::apply)
            .toList();

        return ResponseEntity.ok(pedidos);
    }

    @DeleteMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAll()
    {
        pedidoService.deleteAll();

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/avanzarEstado")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<Void> avanzarEstado(@PathVariable UUID id)
    {
        pedidoService.avanzarEstado(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id)
    {
        pedidoSagaService.cancelarConCompensacion(id, "cancel");

        return ResponseEntity.noContent().build();
    }
}

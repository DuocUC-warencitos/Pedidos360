package io.github.roony11_1.pedidos_service.api.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.roony11_1.pedidos_service.api.dto.response.PedidoDetailResponse;
import io.github.roony11_1.pedidos_service.api.dto.response.PedidoResponse;
import io.github.roony11_1.pedidos_service.api.dto.response.PedidoStatsResponse;
import io.github.roony11_1.pedidos_service.core.application.service.PedidoSagaService;
import io.github.roony11_1.pedidos_service.core.application.service.PedidoService;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/pedidos")
@RequiredArgsConstructor
public class PedidoAdminController
{
    private final PedidoService pedidoService;
    private final PedidoSagaService pedidoSagaService;
    private final Function<Pedido, PedidoResponse> pedidoResponseMapper;
    private final Function<Pedido, PedidoDetailResponse> pedidoDetailResponseMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<Page<PedidoResponse>> findAll(
        @RequestParam Map<String, String> filtros,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) List<String> sort)
    {
        var pagina = pedidoService.findAllAdmin(filtros, page, size, sort);

        return ResponseEntity.ok(pagina.map(pedidoResponseMapper::apply));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<PedidoStatsResponse> stats()
    {
        return ResponseEntity.ok(pedidoService.statsAdmin());
    }

    @GetMapping("/usuario/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<Page<PedidoResponse>> findAllByUserId(
        @PathVariable String userId,
        @RequestParam Map<String, String> filtros,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) List<String> sort)
    {
        var pagina = pedidoService.findAllByUserIdParam(userId, filtros, page, size, sort);

        return ResponseEntity.ok(pagina.map(pedidoResponseMapper::apply));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id)
    {
        pedidoSagaService.cancelarConCompensacion(id, "cancel");

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<PedidoDetailResponse> findById(@PathVariable UUID id)
    {
        var pedido = pedidoService.findByIdAdmin(id);

        return ResponseEntity.ok(pedidoDetailResponseMapper.apply(pedido));
    }

    @PatchMapping("/{id}/avanzarEstado")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<Void> avanzarEstado(@PathVariable UUID id)
    {
        pedidoService.avanzarEstado(id);

        return ResponseEntity.noContent().build();
    }
}

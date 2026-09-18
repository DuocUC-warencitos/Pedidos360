package io.github.roony11_1.pedidos_service.api.controller;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.roony11_1.pedidos_service.api.dto.request.PedidoProductoRequest;
import io.github.roony11_1.pedidos_service.api.dto.response.PedidoResponse;
import io.github.roony11_1.pedidos_service.core.application.service.PedidoService;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/pedidos")
@RequiredArgsConstructor 
public class PedidoController 
{
    private final PedidoService pedidoService;
    private final Function<PedidoProductoRequest, PedidoProducto> pedidoProductoRequestMapper = request -> 
        PedidoProducto.builder()
            .nombreSnapshot(request.getNombreProducto())
            .cantidad(request.getCantidad())
            .build();
            
    private final Function<Pedido, PedidoResponse> pedidoResponseMapper;

    @PostMapping
    public ResponseEntity<PedidoResponse> crearPedido(@RequestBody List<PedidoProductoRequest> request)
    {
        var productos = request.stream()
            .map(pedidoProductoRequestMapper::apply)
            .toList();

        var pedido = pedidoService.save(productos);

        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoResponseMapper.apply(pedido));
    }

    @GetMapping
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
    public ResponseEntity<Void> avanzarEstado(@PathVariable UUID id)
    {
        pedidoService.avanzarEstado(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id)
    {
        pedidoService.cancelar(id);

        return ResponseEntity.noContent().build();
    }
}

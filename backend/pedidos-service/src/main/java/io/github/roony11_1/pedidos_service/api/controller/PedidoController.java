package io.github.roony11_1.pedidos_service.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.roony11_1.pedidos_service.api.dto.request.PedidoProductoRequest;
import io.github.roony11_1.pedidos_service.api.dto.response.PedidoResponse;
import io.github.roony11_1.pedidos_service.core.application.PedidoService;
import io.github.roony11_1.pedidos_service.core.domain.mapper.IMapper;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/pedidos")
@RequiredArgsConstructor 
public class PedidoController 
{
    private final PedidoService pedidoService;
    private final IMapper<PedidoProducto, PedidoProductoRequest> pedidoProductoRequestMapper;
    private final IMapper<PedidoResponse, Pedido> pedidoResponseMapper;

    @PostMapping
    public ResponseEntity<PedidoResponse> save(@RequestBody List<PedidoProductoRequest> request)
    {
        var productos = request.stream()
            .map(pedidoProductoRequestMapper::map)
            .toList();

        var pedido = pedidoService.save(productos);

        return ResponseEntity.ok(pedidoResponseMapper.map(pedido));
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> findAll()
    {
        var pedidos = pedidoService.findAll().stream()
            .map(pedidoResponseMapper::map)
            .toList();

        return ResponseEntity.ok(pedidos);
    }
}

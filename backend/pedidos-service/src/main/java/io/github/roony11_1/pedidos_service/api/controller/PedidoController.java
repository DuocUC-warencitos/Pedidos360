package io.github.roony11_1.pedidos_service.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/pedidos")
@RequiredArgsConstructor 
public class PedidoController 
{
    private final PedidoRepository pedidoRepository;

    @PostMapping
    public ResponseEntity<Pedido> save(@RequestBody Pedido pedido)
    {
        return ResponseEntity.ok(pedidoRepository.save(pedido));
    }
}

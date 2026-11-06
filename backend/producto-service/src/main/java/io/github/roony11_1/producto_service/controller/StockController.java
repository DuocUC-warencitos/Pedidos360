package io.github.roony11_1.producto_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.roony11_1.producto_service.dto.ConfirmarStockRequest;
import io.github.roony11_1.producto_service.dto.LiberarStockRequest;
import io.github.roony11_1.producto_service.dto.ReservaStockRequest;
import io.github.roony11_1.producto_service.dto.ReservaStockResponse;
import io.github.roony11_1.producto_service.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/productos/stock")
@RequiredArgsConstructor 
public class StockController
{
    private final StockService stockService;

    @PostMapping ("/reservar")
    public ResponseEntity<ReservaStockResponse> reservar(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
        @Valid @RequestBody ReservaStockRequest request)
    {
        var response = stockService.reservar(idempotencyKey, correlationId, request);

        return response.isIdempotente() ? ResponseEntity.ok(response) : ResponseEntity.status(201).body(response);
    }

    @PostMapping("/liberar")
    public ResponseEntity<Void> liberar(
        @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
        @Valid @RequestBody LiberarStockRequest request)
    {
        stockService.liberar(correlationId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirmar")
    public ResponseEntity<Void> confirmar(
        @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
        @Valid @RequestBody ConfirmarStockRequest request)
    {
        stockService.confirmar(correlationId, request);
        return ResponseEntity.ok().build();
    }
}

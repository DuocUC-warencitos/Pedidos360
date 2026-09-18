package io.github.roony11_1.pedidos_service.infrastructure.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoClient.ReservaStockRequest;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoClient.ReservaStockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class ProductoClientResiliente 
{
    private final ProductoClient productoClient;

    @CircuitBreaker(name = "productoService", fallbackMethod = "reservarFallback")
    @Retry(name = "productoService")
    public ReservaStockResponse reservar(String idempotencyKey, ReservaStockRequest req)
    {
        return productoClient.reservar(idempotencyKey, req);
    }

    // --- fallbacks ---

    private ReservaStockResponse reservarFallback(String key, ReservaStockRequest req, Throwable t) 
    {
        throw new ProductoServiceUnavailableException("producto-service no disponible al reservar stock: " + t.getMessage(), t);
    }
}
package io.github.roony11_1.pedidos_service.infrastructure.client;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoClient.ReservaStockRequest;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoClient.ReservaStockResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

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
        try
        {
            return productoClient.reservar(idempotencyKey, req);
        }
        catch (FeignException ex)
        {
            if (ex.status() >= 400 && ex.status() < 500)
                throw ReservaStockRechazadaException.fromFeign(ex);
            throw ex;
        }
    }

    @CircuitBreaker(name = "productoService", fallbackMethod = "liberarFallback")
    @Retry(name = "productoService")
    public void liberar(ProductoClient.LiberarStockRequest req)
    {
        try
        {
            productoClient.liberar(req);
        }
        catch (FeignException ex)
        {
            if (ex.status() >= 400 && ex.status() < 500)
                throw ReservaStockRechazadaException.fromFeign(ex);
            throw ex;
        }
    }

    @CircuitBreaker(name = "productoService", fallbackMethod = "confirmarFallback")
    @Retry(name = "productoService")
    public void confirmar(ProductoClient.ConfirmarStockRequest req)
    {
        try
        {
            productoClient.confirmar(req);
        }
        catch (FeignException ex)
        {
            if (ex.status() >= 400 && ex.status() < 500)
                throw ReservaStockRechazadaException.fromFeign(ex);
            throw ex;
        }
    }

    @CircuitBreaker(name = "productoService", fallbackMethod = "obtenerDetallesFallback")
    @Retry(name = "productoService")
    public List<ProductoClient.ProductoSnapshotResponse> obtenerDetalles(List<UUID> productoIds)
    {
        return productoClient.obtenerDetalles(productoIds);
    }

    private List<ProductoClient.ProductoSnapshotResponse> obtenerDetallesFallback(List<UUID> productoIds, Throwable t)
    {
        throw new ProductoServiceUnavailableException("producto-service no disponible al consultar productos: " + t.getMessage(), t);
    }

    // --- fallbacks ---

    private ReservaStockResponse reservarFallback(String key, ReservaStockRequest req, Throwable t) 
    {
        throw new ProductoServiceUnavailableException("producto-service no disponible al reservar stock: " + t.getMessage(), t);
    }

    private void liberarFallback(ProductoClient.LiberarStockRequest req, Throwable t) 
    {
        throw new ProductoServiceUnavailableException("producto-service no disponible al liberar stock: " + t.getMessage(), t);
    }

    private void confirmarFallback(ProductoClient.ConfirmarStockRequest req, Throwable t)
    {
        throw new ProductoServiceUnavailableException("producto-service no disponible al confirmar stock: " + t.getMessage(), t);
    }
}

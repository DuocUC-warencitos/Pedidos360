package io.github.roony11_1.pedidos_service.infrastructure.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "productoService", url = "${app.producto-service.url:http://localhost:8082}", configuration = ProductoClientConfig.class)
public interface ProductoClient 
{
    @PostMapping("/api/v1/productos/stock/reservar")
    ReservaStockResponse reservar(@RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody ReservaStockRequest request);

    @PostMapping("/api/v1/productos/stock/liberar")
    void liberar(@RequestBody LiberarStockRequest request);


    record ReservaStockRequest(UUID pedidoId, List<Item> items) 
    {
        public record Item(UUID productoId, int cantidad) {}
    }

    record ReservaStockResponse(UUID reservaId, UUID pedidoId, String estado,
                                    java.time.Instant creadaEn, boolean idempotente) {}

    record LiberarStockRequest(UUID pedidoId) {}
}

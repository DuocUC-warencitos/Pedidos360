package io.github.roony11_1.producto_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.roony11_1.producto_service.dto.ReservaStockRequest;
import io.github.roony11_1.producto_service.dto.ReservaStockResponse;
import io.github.roony11_1.producto_service.exception.StockInsuficienteException;
import io.github.roony11_1.producto_service.model.EstadoReserva;
import io.github.roony11_1.producto_service.model.ReservaStock;
import io.github.roony11_1.producto_service.repository.ProductoRepository;
import io.github.roony11_1.producto_service.repository.ReservaStockRepository;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor
public class StockService 
{
    private final ProductoRepository productoRepository;
    private final ReservaStockRepository reservaStockRepository;

    @Transactional
    public ReservaStockResponse reservar(String idempotencyKey, ReservaStockRequest request)
    {
        var previa = reservaStockRepository.findByIdempotencyKey(idempotencyKey);

        if (previa.isPresent())
            return toResponse(previa.get(), true);

        for (var producto : request.getProductos())
        {
            var p = productoRepository.findById(producto.getProductoId())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + producto.getProductoId()));

            if (p.getStockDisponible() < producto.getCantidad())
                throw new StockInsuficienteException(p.getId(), producto.getCantidad(), p.getStockDisponible());

            // Esta mapeado con @Version, siguiendo el contexto de Hibernate. Intentara actualizar si el UPDATE difiere en el Version fallara con http 409
            p.reservarStock(producto.getCantidad());
        }

        ReservaStock reserva = ReservaStock.builder()
            .idempotencyKey(idempotencyKey)
            .pedidoId(request.getPedidoId())
            .estado(EstadoReserva.RESERVADO)
            .build();

        reserva = reservaStockRepository.save(reserva);

        return toResponse(reserva, false);
    }

    private ReservaStockResponse toResponse(ReservaStock r, boolean idempotente) 
    {
        return ReservaStockResponse.builder()
            .reservaId(r.getId())
            .pedidoId(r.getPedidoId())
            .estado(r.getEstado())
            .createdAt(r.getCreatedAt())
            .idempotente(idempotente)
            .build();
    }
}

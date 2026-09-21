package io.github.roony11_1.producto_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.roony11_1.producto_service.dto.LiberarStockRequest;
import io.github.roony11_1.producto_service.dto.ReservaStockRequest;
import io.github.roony11_1.producto_service.dto.ReservaStockResponse;
import io.github.roony11_1.error.core.exceptions.NotFoundException;
import io.github.roony11_1.producto_service.exception.StockInsuficienteException;
import io.github.roony11_1.producto_service.model.EstadoReserva;
import io.github.roony11_1.producto_service.model.ReservaStock;
import io.github.roony11_1.producto_service.repository.ProductoRepository;
import io.github.roony11_1.producto_service.repository.ReservaStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

        for (var producto : request.items())
        {
            var p = productoRepository.findById(producto.productoId())
                .orElseThrow(() -> new NotFoundException("Producto", producto.productoId()));

            if (p.getStockDisponible() < producto.cantidad())
                throw new StockInsuficienteException(p.getId(), producto.cantidad(), p.getStockDisponible());

            // Esta mapeado con @Version, siguiendo el contexto de Hibernate. Intentara actualizar si el UPDATE difiere en el Version fallara con http 409
            p.reservarStock(producto.cantidad());
        }

        ReservaStock reserva = ReservaStock.builder()
            .idempotencyKey(idempotencyKey)
            .pedidoId(request.pedidoId())
            .estado(EstadoReserva.RESERVADO)
            .build();

        reserva = reservaStockRepository.save(reserva);

        return toResponse(reserva, false);
    }

    @Transactional
    public void liberar(LiberarStockRequest request) {
        var reservaOpt = reservaStockRepository.findByPedidoId(request.pedidoId());

        if (reservaOpt.isEmpty()) {
            log.warn("Liberar idempotente: no existe reserva para pedidoId={} -> no-op", request.pedidoId());
            return;
        }

        var reserva = reservaOpt.get();

        if (reserva.getEstado() == EstadoReserva.LIBERADO) {
            log.info("Liberar idempotente: pedidoId={} ya LIBERADO -> no-op", request.pedidoId());
            return;
        }

        if (reserva.getEstado() != EstadoReserva.RESERVADO) {
            log.warn("Liberar: pedidoId={} estado={} no es RESERVADO -> no-op", request.pedidoId(), reserva.getEstado());
            return;
        }

        for (var item : request.items()) {
            var p = productoRepository.findById(item.productoId())
                .orElseThrow(() -> new NotFoundException("Producto", item.productoId()));
            p.liberarStock(item.cantidad());
        }

        reserva.setEstado(EstadoReserva.LIBERADO);
        reservaStockRepository.save(reserva);
        log.info("Stock liberado pedidoId={} items={}", request.pedidoId(), request.items().size());
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

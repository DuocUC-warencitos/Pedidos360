package io.github.roony11_1.producto_service.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.roony11_1.producto_service.dto.ConfirmarStockRequest;
import io.github.roony11_1.producto_service.dto.LiberarStockRequest;
import io.github.roony11_1.producto_service.dto.ReservaStockRequest;
import io.github.roony11_1.producto_service.dto.ReservaStockResponse;
import io.github.roony11_1.producto_service.dto.StockItemEvento;
import io.github.roony11_1.producto_service.dto.StockOperacionEvento;
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
public class StockService {

    private final ProductoRepository productoRepository;
    private final ReservaStockRepository reservaStockRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Transactional
    public ReservaStockResponse reservar(
        String idempotencyKey,
        String correlationId,
        ReservaStockRequest request
    ) {

        var previa =
            reservaStockRepository.findByIdempotencyKey(idempotencyKey);

        if (previa.isPresent()) {
            return toResponse(previa.get(), true);
        }

        List<StockItemEvento> items = new ArrayList<>();

        for (var producto : request.items()) {

            var p = productoRepository.findById(producto.productoId())
                .orElseThrow(() ->
                    new NotFoundException("Producto", producto.productoId())
                );

            if (p.getStockDisponible() < producto.cantidad()) {
                throw new StockInsuficienteException(
                    p.getId(),
                    producto.cantidad(),
                    p.getStockDisponible()
                );
            }

            int disponibleAntes = p.getStockDisponible();
            int reservadoAntes = p.getStockReservado();

            p.reservarStock(producto.cantidad());

            items.add(new StockItemEvento(
                p.getId(),
                p.getNombre(),
                p.getSku(),
                p.getCategoria(),
                p.getPrecio(),
                producto.cantidad(),
                disponibleAntes,
                p.getStockDisponible(),
                reservadoAntes,
                p.getStockReservado()
            ));
        }

        ReservaStock reserva = ReservaStock.builder()
            .idempotencyKey(idempotencyKey)
            .pedidoId(request.pedidoId())
            .estado(EstadoReserva.RESERVADO)
            .build();

        reserva = reservaStockRepository.save(reserva);

        kafkaEventPublisher.publicarStock(construirEvento(
            "STOCK_RESERVADO",
            request.pedidoId(),
            idempotencyKey,
            correlationId,
            items
        ));

        return toResponse(reserva, false);
    }

    @Transactional
    public void liberar(String correlationId, LiberarStockRequest request) {

        var reservaOpt =
            reservaStockRepository.findByPedidoId(request.pedidoId());

        if (reservaOpt.isEmpty()) {
            log.warn(
                "Liberar idempotente: no existe reserva para pedidoId={} -> no-op",
                request.pedidoId()
            );
            return;
        }

        var reserva = reservaOpt.get();

        if (reserva.getEstado() == EstadoReserva.LIBERADO) {
            log.info(
                "Liberar idempotente: pedidoId={} ya LIBERADO -> no-op",
                request.pedidoId()
            );
            return;
        }

        if (reserva.getEstado() != EstadoReserva.RESERVADO) {
            log.warn(
                "Liberar: pedidoId={} estado={} no es RESERVADO -> no-op",
                request.pedidoId(),
                reserva.getEstado()
            );
            return;
        }

        List<StockItemEvento> items = new ArrayList<>();

        for (var item : request.items()) {

            var p = productoRepository.findById(item.productoId())
                .orElseThrow(() ->
                    new NotFoundException("Producto", item.productoId())
                );

            int disponibleAntes = p.getStockDisponible();
            int reservadoAntes = p.getStockReservado();

            p.liberarStock(item.cantidad());

            items.add(new StockItemEvento(
                p.getId(),
                p.getNombre(),
                p.getSku(),
                p.getCategoria(),
                p.getPrecio(),
                item.cantidad(),
                disponibleAntes,
                p.getStockDisponible(),
                reservadoAntes,
                p.getStockReservado()
            ));
        }

        reserva.setEstado(EstadoReserva.LIBERADO);
        reservaStockRepository.save(reserva);

        kafkaEventPublisher.publicarStock(construirEvento(
            "STOCK_LIBERADO",
            request.pedidoId(),
            reserva.getIdempotencyKey(),
            correlationId,
            items
        ));

        log.info(
            "Stock liberado pedidoId={} items={}",
            request.pedidoId(),
            request.items().size()
        );
    }

    @Transactional
    public void confirmar(String correlationId, ConfirmarStockRequest request) {

        var reservaOpt =
            reservaStockRepository.findByPedidoId(request.pedidoId());

        if (reservaOpt.isEmpty()) {
            log.warn(
                "Confirmar idempotente: no existe reserva para pedidoId={} -> no-op",
                request.pedidoId()
            );
            return;
        }

        var reserva = reservaOpt.get();

        if (reserva.getEstado() == EstadoReserva.CONFIRMADO) {
            log.info(
                "Confirmar idempotente: pedidoId={} ya CONFIRMADO -> no-op",
                request.pedidoId()
            );
            return;
        }

        if (reserva.getEstado() != EstadoReserva.RESERVADO) {
            log.warn(
                "Confirmar: pedidoId={} estado={} no es RESERVADO -> no-op",
                request.pedidoId(),
                reserva.getEstado()
            );
            return;
        }

        List<StockItemEvento> items = new ArrayList<>();

        for (var item : request.items()) {

            var p = productoRepository.findById(item.productoId())
                .orElseThrow(() ->
                    new NotFoundException("Producto", item.productoId())
                );

            int disponibleAntes = p.getStockDisponible();
            int reservadoAntes = p.getStockReservado();

            p.confirmarStock(item.cantidad());

            items.add(new StockItemEvento(
                p.getId(),
                p.getNombre(),
                p.getSku(),
                p.getCategoria(),
                p.getPrecio(),
                item.cantidad(),
                disponibleAntes,
                p.getStockDisponible(),
                reservadoAntes,
                p.getStockReservado()
            ));
        }

        reserva.setEstado(EstadoReserva.CONFIRMADO);
        reservaStockRepository.save(reserva);

        kafkaEventPublisher.publicarStock(construirEvento(
            "STOCK_CONFIRMADO",
            request.pedidoId(),
            reserva.getIdempotencyKey(),
            correlationId,
            items
        ));

        log.info(
            "Stock confirmado pedidoId={} items={}",
            request.pedidoId(),
            request.items().size()
        );
    }

    private StockOperacionEvento construirEvento(
        String tipo,
        UUID pedidoId,
        String idempotencyKey,
        String correlationId,
        List<StockItemEvento> items
    ) {
        String operacionId = pedidoId + ":" + tipo + ":" + idempotencyKey;

        return new StockOperacionEvento(
            UUID.nameUUIDFromBytes(operacionId.getBytes(StandardCharsets.UTF_8)),
            StockOperacionEvento.SCHEMA_VERSION,
            tipo,
            operacionId,
            pedidoId,
            idempotencyKey,
            correlacion(correlationId, pedidoId),
            Instant.now(),
            StockOperacionEvento.ORIGEN,
            List.copyOf(items)
        );
    }

    private String correlacion(String correlationId, UUID pedidoId) {
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }

        return pedidoId.toString();
    }

    private ReservaStockResponse toResponse(
        ReservaStock r,
        boolean idempotente
    ) {
        return ReservaStockResponse.builder()
            .reservaId(r.getId())
            .pedidoId(r.getPedidoId())
            .estado(r.getEstado())
            .createdAt(r.getCreatedAt())
            .idempotente(idempotente)
            .build();
    }
}

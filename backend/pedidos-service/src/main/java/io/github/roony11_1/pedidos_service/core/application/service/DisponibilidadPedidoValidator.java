package io.github.roony11_1.pedidos_service.core.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoClient;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoClientResiliente;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Centraliza TODA la lógica de disponibilidad previa a persistir un pedido:
 * cantidad válida, existencia, activo y stock suficiente.
 * <p>
 * Best-effort: evita guardar pedidos condenados al fallo, pero la reserva
 * async sigue siendo la verdad final (no elimina la race entre compradores).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisponibilidadPedidoValidator
{
    private final ProductoClientResiliente productoClientResiliente;

    public PrevalidacionResultado prevalidar(List<PedidoProducto> items)
    {
        for (var p : items)
        {
            if (p.getCantidad() <= 0)
                throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }

        var productoIds = items.stream()
            .map(PedidoProducto::getProductoId)
            .distinct()
            .toList();

        var detalles = productoClientResiliente.obtenerDetalles(productoIds);

        Map<UUID, ProductoClient.ProductoSnapshotResponse> porId = detalles.stream()
            .collect(Collectors.toMap(
                ProductoClient.ProductoSnapshotResponse::id,
                Function.identity(),
                (a, b) -> a));

        var snapshot = new ArrayList<PedidoProducto>();
        var faltantes = new ArrayList<UUID>();
        var sinStock = new ArrayList<ItemSinStock>();

        for (var item : items)
        {
            var detalle = porId.get(item.getProductoId());

            if (detalle == null || !detalle.activo())
            {
                if (!faltantes.contains(item.getProductoId()))
                    faltantes.add(item.getProductoId());
                continue;
            }

            if (item.getCantidad() > detalle.stockDisponible())
            {
                sinStock.add(new ItemSinStock(
                    item.getProductoId(), item.getCantidad(), detalle.stockDisponible()));
                continue;
            }

            snapshot.add(PedidoProducto.builder()
                .productoId(item.getProductoId())
                .nombreSnapshot(detalle.nombre())
                .precioSnapshot(detalle.precio())
                .cantidad(item.getCantidad())
                .build());
        }

        if (!faltantes.isEmpty() || !sinStock.isEmpty())
            log.info("Prevalidación con problemas faltantes={} sinStock={}", faltantes.size(), sinStock.size());

        return new PrevalidacionResultado(
            List.copyOf(snapshot), List.copyOf(faltantes), List.copyOf(sinStock));
    }

    public record PrevalidacionResultado(
        List<PedidoProducto> snapshot,
        List<UUID> faltantes,
        List<ItemSinStock> sinStock)
    {
        public boolean tieneProblemas()
        {
            return !faltantes.isEmpty() || !sinStock.isEmpty();
        }
    }

    public record ItemSinStock(UUID productoId, int solicitado, int disponible) {}
}

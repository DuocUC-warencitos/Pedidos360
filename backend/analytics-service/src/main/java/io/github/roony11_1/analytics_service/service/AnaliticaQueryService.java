package io.github.roony11_1.analytics_service.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import io.github.roony11_1.analytics_service.dto.AlertaStockResponse;
import io.github.roony11_1.analytics_service.dto.AuditoriaItemResponse;
import io.github.roony11_1.analytics_service.dto.EstadoStreamsResponse;
import io.github.roony11_1.analytics_service.dto.MovimientoDiaResponse;
import io.github.roony11_1.analytics_service.dto.ResumenAnaliticaResponse;
import io.github.roony11_1.analytics_service.dto.TopProductoResponse;
import io.github.roony11_1.analytics_service.streams.AuditoriaItem;
import io.github.roony11_1.analytics_service.streams.InventarioProducto;
import io.github.roony11_1.analytics_service.streams.MovimientoProductoDia;
import io.github.roony11_1.analytics_service.streams.StoreNames;

@Service
public class AnaliticaQueryService 
{
    private static final ZoneOffset ZONA = ZoneOffset.UTC;
    private static final String CONFIRMADO = "STOCK_CONFIRMADO";

    private final StreamsBuilderFactoryBean streamsFactory;

    public AnaliticaQueryService(StreamsBuilderFactoryBean streamsFactory) 
    {
        this.streamsFactory = streamsFactory;
    }

    public ResumenAnaliticaResponse resumen() 
    {
        List<InventarioProducto> inventario = leerInventario();
        List<MovimientoProductoDia> movimientos = leerMovimientos();

        String mesActual = LocalDate.now(ZONA).toString().substring(0, 7);

        long unidadesMes = movimientos.stream()
            .filter(m -> CONFIRMADO.equals(m.tipo()))
            .filter(m -> m.fecha().startsWith(mesActual))
            .mapToLong(MovimientoProductoDia::unidades)
            .sum();

        long ingresosMes = movimientos.stream()
            .filter(m -> CONFIRMADO.equals(m.tipo()))
            .filter(m -> m.fecha().startsWith(mesActual))
            .mapToLong(MovimientoProductoDia::ingresos)
            .sum();

        return new ResumenAnaliticaResponse(
            inventario.size(),
            inventario.stream().filter(InventarioProducto::activo).count(),
            inventario.stream().mapToLong(InventarioProducto::stockDisponible).sum(),
            inventario.stream().mapToLong(InventarioProducto::stockReservado).sum(),
            alertasDe(inventario).size(),
            unidadesMes,
            ingresosMes
        );
    }

    public List<AlertaStockResponse> alertas() 
    {
        return alertasDe(leerInventario());
    }

    public List<TopProductoResponse> top(
        LocalDate desde,
        LocalDate hasta,
        int limit) 
    {
        LocalDate inicio = desde != null ? desde : LocalDate.now(ZONA).minusMonths(11).withDayOfMonth(1);
        LocalDate fin = hasta != null ? hasta : LocalDate.now(ZONA);

        Map<UUID, TopAcumulado> acumulados = new LinkedHashMap<>();

        for (MovimientoProductoDia movimiento : leerMovimientos())
        {
            if (!CONFIRMADO.equals(movimiento.tipo())) 
            {
                continue;
            }

            if (!enRango(movimiento.fecha(), inicio, fin)) 
            {
                continue;
            }

            TopAcumulado acumulado = acumulados.computeIfAbsent(
                movimiento.productoId(),
                id -> new TopAcumulado()
            );

            acumulado.unidades += movimiento.unidades();
            acumulado.ingresos += movimiento.ingresos();

            if (acumulado.nombre == null) 
            {
                acumulado.nombre = movimiento.nombre();
            }

            if (acumulado.sku == null) 
            {
                acumulado.sku = movimiento.sku();
            }
        }

        return acumulados.entrySet().stream()
            .map(entry -> new TopProductoResponse(
                entry.getKey(),
                entry.getValue().nombre,
                entry.getValue().sku,
                entry.getValue().unidades,
                entry.getValue().ingresos
            ))
            .sorted(
                Comparator.comparingLong(TopProductoResponse::unidades)
                    .reversed()
                    .thenComparing(
                        Comparator.comparingLong(TopProductoResponse::ingresos)
                            .reversed()
                    )
            )
            .limit(Math.max(1, limit))
            .toList();
    }

    public List<MovimientoDiaResponse> movimientos(
        LocalDate desde,
        LocalDate hasta) 
    {
        LocalDate inicio = desde != null ? desde : LocalDate.now(ZONA).minusMonths(11).withDayOfMonth(1);
        LocalDate fin = hasta != null ? hasta : LocalDate.now(ZONA);

        Map<String, long[]> acumulados = new HashMap<>();

        for (MovimientoProductoDia movimiento : leerMovimientos()) {
            if (!enRango(movimiento.fecha(), inicio, fin)) {
                continue;
            }

            String key = movimiento.fecha() + "|" + movimiento.tipo();
            long[] valores = acumulados.computeIfAbsent(key, k -> new long[3]);

            valores[0] += movimiento.unidades();
            valores[1] += movimiento.ingresos();
            valores[2] += movimiento.operaciones();
        }

        List<MovimientoDiaResponse> respuesta = new ArrayList<>();

        for (Map.Entry<String, long[]> entry : acumulados.entrySet()) {
            String[] partes = entry.getKey().split("\\|", 2);
            long[] valores = entry.getValue();

            respuesta.add(new MovimientoDiaResponse(
                partes[0],
                partes[1],
                valores[0],
                valores[1],
                valores[2]
            ));
        }

        return respuesta.stream()
            .sorted(
                Comparator.comparing(MovimientoDiaResponse::fecha)
                    .thenComparing(MovimientoDiaResponse::tipo)
            )
            .toList();
    }

    public List<AuditoriaItemResponse> auditoria(
        LocalDate desde,
        LocalDate hasta,
        int limit) 
    {
        LocalDate inicio = desde != null ? desde : LocalDate.now(ZONA).minusDays(30);
        LocalDate fin = hasta != null ? hasta : LocalDate.now(ZONA);

        Instant from = inicio.atStartOfDay(ZONA).toInstant();
        Instant to = fin.plusDays(1).atStartOfDay(ZONA).toInstant();

        List<AuditoriaItem> items = conStore(() -> 
        {
            ReadOnlyWindowStore<String, AuditoriaItem> store = auditoriaStore();
            List<AuditoriaItem> encontrados = new ArrayList<>();

            try (KeyValueIterator<Windowed<String>, AuditoriaItem> iterator =
                     store.fetchAll(from, to)) 
            {
                while (iterator.hasNext()) 
                {
                    encontrados.add(iterator.next().value);
                }
            }

            return encontrados;
        });

        return items.stream()
            .sorted(
                Comparator.comparing(AuditoriaItem::ocurridoEn)
                    .reversed()
            )
            .limit(Math.max(1, limit))
            .map(this::toResponse)
            .toList();
    }

    public EstadoStreamsResponse estado() 
    {
        List<InventarioProducto> inventario = leerInventario();
        List<MovimientoProductoDia> movimientos = leerMovimientos();
        List<AuditoriaItemResponse> eventos = auditoria(
            LocalDate.now(ZONA).minusDays(30),
            LocalDate.now(ZONA),
            1000
        );

        return new EstadoStreamsResponse(
            streams().state().name(),
            List.of(
                StoreNames.INVENTARIO,
                StoreNames.PRODUCTO_DEDUP,
                StoreNames.STOCK_DEDUP,
                StoreNames.MOVIMIENTOS,
                StoreNames.AUDITORIA
            ),
            inventario.size(),
            movimientos.size(),
            eventos.size()
        );
    }

    private List<AlertaStockResponse> alertasDe(
        List<InventarioProducto> inventario) 
    {
        return inventario.stream()
            .filter(InventarioProducto::activo)
            .filter(this::esAlerta)
            .sorted(
                Comparator.comparingInt(InventarioProducto::stockDisponible)
                    .thenComparing(
                        InventarioProducto::nombre,
                        Comparator.nullsLast(String::compareToIgnoreCase)
                    )
            )
            .map(p -> new AlertaStockResponse(
                p.productoId(),
                p.nombre(),
                p.sku(),
                p.stockDisponible(),
                p.stockMinimo()
            ))
            .toList();
    }

    private boolean esAlerta(InventarioProducto producto) 
    {
        return producto.stockDisponible() <= 0
            || (producto.stockMinimo() != null
                && producto.stockDisponible() < producto.stockMinimo());
    }

    private List<InventarioProducto> leerInventario() 
    {
        return conStore(() -> {
            ReadOnlyKeyValueStore<String, InventarioProducto> store =
                inventarioStore();
            List<InventarioProducto> productos = new ArrayList<>();

            try (KeyValueIterator<String, InventarioProducto> iterator = store.all()) {
                while (iterator.hasNext()) {
                    productos.add(iterator.next().value);
                }
            }

            return productos;
        });
    }

    private List<MovimientoProductoDia> leerMovimientos() 
    {
        return conStore(() -> {
            ReadOnlyKeyValueStore<String, MovimientoProductoDia> store =
                movimientosStore();
            List<MovimientoProductoDia> movimientos = new ArrayList<>();

            try (KeyValueIterator<String, MovimientoProductoDia> iterator =
                     store.all()) {
                while (iterator.hasNext()) {
                    movimientos.add(iterator.next().value);
                }
            }

            return movimientos;
        });
    }

    private boolean enRango(
        String fecha,
        LocalDate desde,
        LocalDate hasta) 
    {
        return fecha.compareTo(desde.toString()) >= 0
            && fecha.compareTo(hasta.toString()) <= 0;
    }

    private ReadOnlyKeyValueStore<String, InventarioProducto> inventarioStore() 
    {
        return streams().store(
            StoreQueryParameters.fromNameAndType(
                StoreNames.INVENTARIO,
                QueryableStoreTypes.<String, InventarioProducto>keyValueStore()
            )
        );
    }

    private ReadOnlyKeyValueStore<String, MovimientoProductoDia> movimientosStore() 
    {
        return streams().store(
            StoreQueryParameters.fromNameAndType(
                StoreNames.MOVIMIENTOS,
                QueryableStoreTypes.<String, MovimientoProductoDia>keyValueStore()
            )
        );
    }

    private ReadOnlyWindowStore<String, AuditoriaItem> auditoriaStore() 
    {
        return streams().store(
            StoreQueryParameters.fromNameAndType(
                StoreNames.AUDITORIA,
                QueryableStoreTypes.<String, AuditoriaItem>windowStore()
            )
        );
    }

    private KafkaStreams streams() 
    {
        KafkaStreams streams = streamsFactory.getKafkaStreams();

        if (streams == null) 
        {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Kafka Streams todavía no está listo"
            );
        }

        return streams;
    }

    private <T> T conStore(Supplier<T> supplier) 
    {
        try {
            return supplier.get();
        } catch (InvalidStateStoreException ex) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Kafka Streams todavía no está listo",
                ex
            );
        }
    }

    private AuditoriaItemResponse toResponse(AuditoriaItem item) 
    {
        return new AuditoriaItemResponse(
            item.eventoId(),
            item.tipo(),
            item.productoId(),
            item.nombre(),
            item.sku(),
            item.pedidoId(),
            item.operacionId(),
            item.cantidad(),
            item.stockDisponibleAntes(),
            item.stockDisponibleDespues(),
            item.stockReservadoAntes(),
            item.stockReservadoDespues(),
            item.ocurridoEn(),
            item.correlationId(),
            item.usuarioId()
        );
    }

    private static final class TopAcumulado 
    {
        private long unidades;
        private long ingresos;
        private String nombre;
        private String sku;
    }
}

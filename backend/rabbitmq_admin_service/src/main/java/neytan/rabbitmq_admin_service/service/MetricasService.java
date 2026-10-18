package neytan.rabbitmq_admin_service.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import neytan.rabbitmq_admin_service.dto.EventoReciente;
import neytan.rabbitmq_admin_service.dto.ProductoRanking;
import neytan.rabbitmq_admin_service.dto.PuntoDiario;
import neytan.rabbitmq_admin_service.model.MetricaDiaria;
import neytan.rabbitmq_admin_service.model.ProductoComprado;
import neytan.rabbitmq_admin_service.repository.EventoAuditoriaRepository;
import neytan.rabbitmq_admin_service.repository.MetricaDiariaRepository;
import neytan.rabbitmq_admin_service.repository.ProductoCompradoRepository;

@Service
public class MetricasService {

    private final MetricaDiariaRepository metricaDiariaRepository;
    private final ProductoCompradoRepository productoCompradoRepository;
    private final EventoAuditoriaRepository eventoAuditoriaRepository;

    public MetricasService(
            MetricaDiariaRepository metricaDiariaRepository,
            ProductoCompradoRepository productoCompradoRepository,
            EventoAuditoriaRepository eventoAuditoriaRepository) {
        this.metricaDiariaRepository = metricaDiariaRepository;
        this.productoCompradoRepository = productoCompradoRepository;
        this.eventoAuditoriaRepository = eventoAuditoriaRepository;
    }

    @Transactional
    public void registrarEvento(String routingKey) {
        LocalDate hoy = LocalDate.now();

        MetricaDiaria metrica = metricaDiariaRepository
                .findByRoutingKeyAndFecha(routingKey, hoy)
                .orElseGet(() -> {
                    MetricaDiaria nueva = new MetricaDiaria();
                    nueva.setRoutingKey(routingKey);
                    nueva.setFecha(hoy);
                    return nueva;
                });

        metrica.setTotal(metrica.getTotal() + 1);
        metricaDiariaRepository.save(metrica);
    }

    public void registrarProductos(List<ProductoComprado> productos) {
        productoCompradoRepository.saveAll(productos);
    }

    public List<PuntoDiario> obtenerDiarias(int meses) {
        int ventana = Math.max(1, Math.min(meses, 24));
        LocalDate desde = LocalDate.now().withDayOfMonth(1).minusMonths(ventana - 1L);

        return metricaDiariaRepository
                .findByFechaGreaterThanEqualOrderByFechaAsc(desde)
                .stream()
                .map(m -> new PuntoDiario(m.getFecha(), m.getRoutingKey(), m.getTotal()))
                .toList();
    }

    public List<ProductoRanking> obtenerRanking(LocalDate desde, LocalDate hasta, int limit) {
        int tope = Math.max(1, Math.min(limit, 50));

        return productoCompradoRepository
                .rankingPorPeriodo(desde, hasta, PageRequest.of(0, tope))
                .stream()
                .map(fila -> new ProductoRanking(
                        (UUID) fila[0],
                        ((Number) fila[1]).longValue()))
                .toList();
    }

    public List<EventoReciente> obtenerUltimos(int limit) {
        int tope = Math.max(1, Math.min(limit, 100));

        return eventoAuditoriaRepository
                .findAll(PageRequest.of(0, tope, Sort.by(Sort.Direction.DESC, "recibidoAt")))
                .getContent()
                .stream()
                .map(e -> new EventoReciente(
                        e.getRecibidoAt(),
                        e.getRoutingKey(),
                        e.getPedidoId(),
                        e.getCorrelationId()))
                .toList();
    }
}

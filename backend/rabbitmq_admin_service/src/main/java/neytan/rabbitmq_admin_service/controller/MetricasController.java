package neytan.rabbitmq_admin_service.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import neytan.rabbitmq_admin_service.dto.EventoReciente;
import neytan.rabbitmq_admin_service.dto.ProductoRanking;
import neytan.rabbitmq_admin_service.dto.PuntoDiario;
import neytan.rabbitmq_admin_service.service.MetricasService;

@RestController
@RequestMapping("/api/admin/metricas")
@PreAuthorize("hasRole('ADMIN')")
public class MetricasController {

    private final MetricasService metricasService;

    public MetricasController(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    @GetMapping("/diarias")
    public ResponseEntity<List<PuntoDiario>> listarDiarias(
            @RequestParam(defaultValue = "12") int meses) {

        return ResponseEntity.ok(metricasService.obtenerDiarias(meses));
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<ProductoRanking>> listarRanking(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(metricasService.obtenerRanking(desde, hasta, limit));
    }

    @GetMapping("/ultimos")
    public ResponseEntity<List<EventoReciente>> listarUltimos(
            @RequestParam(defaultValue = "20") int limit) {

        return ResponseEntity.ok(metricasService.obtenerUltimos(limit));
    }
}

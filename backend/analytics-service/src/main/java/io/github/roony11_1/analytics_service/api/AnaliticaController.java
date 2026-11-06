package io.github.roony11_1.analytics_service.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.roony11_1.analytics_service.dto.AlertaStockResponse;
import io.github.roony11_1.analytics_service.dto.AuditoriaItemResponse;
import io.github.roony11_1.analytics_service.dto.EstadoStreamsResponse;
import io.github.roony11_1.analytics_service.dto.MovimientoDiaResponse;
import io.github.roony11_1.analytics_service.dto.ResumenAnaliticaResponse;
import io.github.roony11_1.analytics_service.dto.TopProductoResponse;
import io.github.roony11_1.analytics_service.service.AnaliticaQueryService;

@RestController
@RequestMapping("/api/v1/admin/analiticas")
public class AnaliticaController 
{
    private final AnaliticaQueryService analiticaQueryService;

    public AnaliticaController(AnaliticaQueryService analiticaQueryService) 
    {
        this.analiticaQueryService = analiticaQueryService;
    }

    @GetMapping("/inventario/resumen")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ResumenAnaliticaResponse> resumen() 
    {
        return ResponseEntity.ok(analiticaQueryService.resumen());
    }

    @GetMapping("/inventario/alertas")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<List<AlertaStockResponse>> alertas() 
    {
        return ResponseEntity.ok(analiticaQueryService.alertas());
    }

    @GetMapping("/inventario/top")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<List<TopProductoResponse>> top(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(defaultValue = "10") int limit) 
    {
        return ResponseEntity.ok(analiticaQueryService.top(desde, hasta, limit));
    }

    @GetMapping("/inventario/movimientos")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<List<MovimientoDiaResponse>> movimientos(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) 
        {
        return ResponseEntity.ok(analiticaQueryService.movimientos(desde, hasta));
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<List<AuditoriaItemResponse>> auditoria(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(defaultValue = "20") int limit) 
    {
        return ResponseEntity.ok(
            analiticaQueryService.auditoria(desde, hasta, limit)
        );
    }

    @GetMapping("/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<EstadoStreamsResponse> estado() 
    {
        return ResponseEntity.ok(analiticaQueryService.estado());
    }
}

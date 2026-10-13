package io.github.roony11_1.producto_service.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.roony11_1.producto_service.dto.ProductoDetailResponse;
import io.github.roony11_1.producto_service.dto.ProductoRequest;
import io.github.roony11_1.producto_service.dto.ProductoResponse;
import io.github.roony11_1.producto_service.dto.ProductoStatsResponse;
import io.github.roony11_1.producto_service.mappers.ProductoMappers;
import io.github.roony11_1.producto_service.model.Producto;
import io.github.roony11_1.producto_service.service.ProductoAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/v1/admin/productos")
@RequiredArgsConstructor 
public class ProductoAdminController 
{
    private final ProductoAdminService productoAdminService;
    private final ProductoMappers mappers;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<Page<ProductoResponse>> findAll(
        @RequestParam Map<String, String> filtros,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) List<String> sort)
    {
        var pagina = productoAdminService.findAll(filtros, page, size, sort);

        return ResponseEntity.ok(pagina.map(mappers.productoToResponse::apply));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ProductoStatsResponse> stats()
    {
        return ResponseEntity.ok(productoAdminService.stats());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ProductoDetailResponse> obtenerPorId(@PathVariable UUID id)
    {
        return productoAdminService.obtenerPorId(id)
            .map(mappers.productoToDetailResponse::apply)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request)
    {
        Producto producto = mappers.requestToProducto.apply(request);
        Producto guardado = productoAdminService.crear(producto);
        ProductoResponse response = mappers.productoToResponse.apply(guardado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable UUID id, @jakarta.validation.Valid @RequestBody ProductoRequest request)
    {
        Producto producto = mappers.requestToProducto.apply(request);

        if (request.getActivo() == null)
        {
            producto.setActivo(
                productoAdminService.obtenerPorId(id)
                    .map(Producto::isActivo)
                    .orElse(true)
            );
        }

        return productoAdminService.actualizar(id, producto)
            .map(mappers.productoToResponse::apply)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id)
    {
        boolean eliminado = productoAdminService.eliminar(id);
        if (!eliminado)
            return ResponseEntity.notFound().build();
        
        return ResponseEntity.noContent().build();
    }
}

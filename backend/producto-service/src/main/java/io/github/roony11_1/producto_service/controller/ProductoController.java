package io.github.roony11_1.producto_service.controller;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.springframework.web.bind.annotation.RestController;

import io.github.roony11_1.producto_service.dto.ProductoDetailResponse;
import io.github.roony11_1.producto_service.dto.ProductoRequest;
import io.github.roony11_1.producto_service.dto.ProductoResponse;
import io.github.roony11_1.producto_service.model.Producto;
import io.github.roony11_1.producto_service.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController
{
    private final ProductoService productoService;

    private final Function<ProductoRequest, Producto> requestToProducto = request ->
        Producto.builder()
            .nombre(request.getNombre())
            .precio(request.getPrecio())
            .stockDisponible(request.getStock())
            .build();

    private final Function<Producto, ProductoResponse> productoToResponse = producto ->
           ProductoResponse.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .precio(producto.getPrecio())
                .stockDisponible(producto.getStockDisponible())
                .build();

    private final Function<Producto, ProductoDetailResponse> productoToDetailResponse = producto ->
           ProductoDetailResponse.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .precio(producto.getPrecio())
                .stockDisponible(producto.getStockDisponible())
                .stockReservado(producto.getStockReservado())
                .build();

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar()
    {
        return ResponseEntity.ok(
            productoService.listar()
                .stream()
                .map(productoToResponse::apply)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoDetailResponse> obtenerPorId(@PathVariable UUID id) {
        return productoService.obtenerPorId(id)
            .map(productoToDetailResponse::apply)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request)
    {
        Producto producto = requestToProducto.apply(request);
        Producto guardado = productoService.crear(producto);
        ProductoResponse response = productoToResponse.apply(guardado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable UUID id, @jakarta.validation.Valid @RequestBody ProductoRequest request)
    {
        Producto producto = requestToProducto.apply(request);

        return productoService.actualizar(id, producto)
            .map(productoToResponse::apply)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id)
    {
        boolean eliminado = productoService.eliminar(id);
        if (!eliminado) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}

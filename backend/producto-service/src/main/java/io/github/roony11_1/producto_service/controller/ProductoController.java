package io.github.roony11_1.producto_service.controller;

import java.util.List;
import java.util.Map;
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
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.roony11_1.producto_service.dto.ProductoDetailResponse;
import io.github.roony11_1.producto_service.dto.ProductoRequest;
import io.github.roony11_1.producto_service.dto.ProductoResponse;
import io.github.roony11_1.producto_service.dto.ProductoSnapshotResponse;
import io.github.roony11_1.producto_service.mappers.ProductoMappers;
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
    private final ProductoMappers mappers;

    private final Function<Producto, ProductoSnapshotResponse> prodcutoSnapshotMapper = producto ->
        ProductoSnapshotResponse.builder()
        .id(producto.getId())
        .nombre(producto.getNombre())
        .precio(producto.getPrecio())
        .build();


    @PostMapping("/detalles")
    public ResponseEntity<List<ProductoSnapshotResponse>> obtenerDetalles(@RequestBody List<UUID> ids)
    {
        List<Producto> productos = productoService.obtenerPorIds(ids);

        var response = productos.stream()
            .map(prodcutoSnapshotMapper::apply)
            .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ProductoResponse>> listar(
        @RequestParam Map<String, String> filtros,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) List<String> sort)
    {
        var pagina = productoService.listar(filtros, page, size, sort);

        return ResponseEntity.ok(pagina.map(mappers.productoToResponse::apply));
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<String>> listarCategorias()
    {
        return ResponseEntity.ok(productoService.listarCategorias());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoDetailResponse> obtenerPorId(@PathVariable UUID id) {
        return productoService.obtenerPorId(id)
            .map(mappers.productoToDetailResponse::apply)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}

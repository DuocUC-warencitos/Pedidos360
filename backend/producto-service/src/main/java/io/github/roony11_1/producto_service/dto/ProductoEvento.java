package io.github.roony11_1.producto_service.dto;

import java.time.Instant;
import java.util.UUID;

import io.github.roony11_1.producto_service.model.Producto;

public record ProductoEvento(
    UUID id,
    int precio,
    String nombre,
    String sku,
    String categoria,
    String descripcion,
    String shortDescription,
    int stockDisponible,
    int stockReservado,
    Integer stockMinimo,
    boolean activo,
    Instant createdAt,
    Instant updatedAt,
    Long version
) {
    public static ProductoEvento desde(Producto producto) {
        return new ProductoEvento(
            producto.getId(),
            producto.getPrecio(),
            producto.getNombre(),
            producto.getSku(),
            producto.getCategoria(),
            producto.getDescripcion(),
            producto.getShortDescription(),
            producto.getStockDisponible(),
            producto.getStockReservado(),
            producto.getStockMinimo(),
            producto.isActivo(),
            producto.getCreatedAt(),
            producto.getUpdatedAt(),
            producto.getVersion()
        );
    }
}
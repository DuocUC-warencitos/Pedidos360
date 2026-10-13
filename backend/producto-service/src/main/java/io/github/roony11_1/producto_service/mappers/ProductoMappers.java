package io.github.roony11_1.producto_service.mappers;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import io.github.roony11_1.producto_service.dto.ProductoDetailResponse;
import io.github.roony11_1.producto_service.dto.ProductoRequest;
import io.github.roony11_1.producto_service.dto.ProductoResponse;
import io.github.roony11_1.producto_service.model.Producto;

@Component
public class ProductoMappers 
{
    public final Function<ProductoRequest, Producto> requestToProducto = request ->
        Producto.builder()
            .nombre(request.getNombre())
            .precio(request.getPrecio())
            .sku(request.getSku())
            .categoria(request.getCategoria())
            .descripcion(request.getDescripcion())
            .shortDescription(request.getShortDescription())
            .stockDisponible(request.getStock())
            .stockReservado(0)
            .stockMinimo(request.getStockMinimo())
            .activo(request.getActivo() != null ? request.getActivo() : true)
            .build();

    public final Function<Producto, ProductoResponse> productoToResponse = producto ->
        ProductoResponse.builder()
            .id(producto.getId())
            .nombre(producto.getNombre())
            .precio(producto.getPrecio())
            .stockDisponible(producto.getStockDisponible())
            .sku(producto.getSku())
            .shortDescription(producto.getShortDescription())
            .activo(producto.isActivo())
            .build();

    public final Function<Producto, ProductoDetailResponse> productoToDetailResponse = producto ->
        ProductoDetailResponse.builder()
            .id(producto.getId())
            .nombre(producto.getNombre())
            .precio(producto.getPrecio())
            .stockDisponible(producto.getStockDisponible())
            .stockReservado(producto.getStockReservado())
            .sku(producto.getSku())
            .categoria(producto.getCategoria())
            .descripcion(producto.getDescripcion())
            .shortDescription(producto.getShortDescription())
            .stockMinimo(producto.getStockMinimo())
            .activo(producto.isActivo())
            .createdAt(producto.getCreatedAt())
            .updatedAt(producto.getUpdatedAt())
            .build();
}

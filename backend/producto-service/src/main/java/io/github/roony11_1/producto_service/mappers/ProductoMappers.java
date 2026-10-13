package io.github.roony11_1.producto_service.mappers;

import java.util.function.Function;

import org.springframework.stereotype.Component;

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
            .stockDisponible(request.getStock())
            .stockReservado(0)
            .stockMinimo(request.getStockMinimo())
            .activo(true)
            .build();

    public final Function<Producto, ProductoResponse> productoToResponse = producto ->
        ProductoResponse.builder()
            .id(producto.getId())
            .nombre(producto.getNombre())
            .precio(producto.getPrecio())
            .stockDisponible(producto.getStockDisponible())
            .sku(producto.getSku())
            .activo(producto.isActivo())
            .build();
}

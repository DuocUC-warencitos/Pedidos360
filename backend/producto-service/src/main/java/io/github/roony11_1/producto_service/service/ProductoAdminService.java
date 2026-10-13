package io.github.roony11_1.producto_service.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import io.github.roony11_1.producto_service.model.Producto;
import io.github.roony11_1.producto_service.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoAdminService 
{
    private final ProductoRepository productoRepository;

    public Producto crear(Producto producto)
    {
        return productoRepository.save(producto);
    }

    public Optional<Producto> actualizar(UUID id, Producto producto)
    {
        return productoRepository.findById(id)
                .map(existente ->
                {
                    existente.setNombre(producto.getNombre());
                    existente.setPrecio(producto.getPrecio());
                    existente.setStockDisponible(producto.getStockDisponible());
                    return productoRepository.save(existente);
                });
    }

    public boolean eliminar(UUID id)
    {
        return productoRepository.findById(id)
                .map(p ->
                {
                    productoRepository.delete(p);
                    return true;
                })
                .orElse(false);
    }
}

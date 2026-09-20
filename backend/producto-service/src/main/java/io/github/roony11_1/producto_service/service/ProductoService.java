package io.github.roony11_1.producto_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import io.github.roony11_1.producto_service.model.Producto;
import io.github.roony11_1.producto_service.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoService
{
    private final ProductoRepository productoRepository;

    public List<Producto> listar()
    {
        return productoRepository.findAll();
    }

    public Optional<Producto> obtenerPorId(UUID id)
    {
        return productoRepository.findById(id);
    }

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

    // Compatibilidad con nombres anteriores si se usan en tests
    public List<Producto> findAll()
    {
        return listar();
    }

    public Optional<Producto> findById(UUID id)
    {
        return obtenerPorId(id);
    }

    public Producto save(Producto producto)
    {
        return crear(producto);
    }
}

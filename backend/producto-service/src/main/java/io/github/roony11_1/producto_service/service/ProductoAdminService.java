package io.github.roony11_1.producto_service.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import io.github.roony11_1.producto_service.dto.ProductoStatsResponse;
import io.github.roony11_1.producto_service.infrastructure.spec.ProductoFiltrosParser;
import io.github.roony11_1.producto_service.model.Producto;
import io.github.roony11_1.producto_service.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoAdminService
{
    private final ProductoRepository productoRepository;

    public Page<Producto> findAll(Map<String, String> filtros, int page, int size, List<String> sort)
    {
        Specification<Producto> specification = ProductoFiltrosParser.parse(filtros, null);
        Pageable pageable = ProductoFiltrosParser.pageable(page, size, sort, Sort.by(Sort.Direction.DESC, "createdAt"));

        return productoRepository.findAll(specification, pageable);
    }

    public ProductoStatsResponse stats()
    {
        return ProductoStatsResponse.builder()
            .total(productoRepository.count())
            .activos(productoRepository.contarActivos())
            .stockTotal(productoRepository.sumarStockDisponible())
            .stockReservadoTotal(productoRepository.sumarStockReservado())
            .build();
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
                    existente.setSku(producto.getSku());
                    existente.setCategoria(producto.getCategoria());
                    existente.setDescripcion(producto.getDescripcion());
                    existente.setShortDescription(producto.getShortDescription());
                    existente.setStockMinimo(producto.getStockMinimo());
                    existente.setActivo(producto.isActivo());
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

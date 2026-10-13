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

import io.github.roony11_1.producto_service.infrastructure.spec.ProductoFiltrosParser;
import io.github.roony11_1.producto_service.model.Producto;
import io.github.roony11_1.producto_service.repository.ProductoRepository;
import io.github.roony11_1.specification.core.FilterCondition;
import io.github.roony11_1.specification.core.FilterOperator;
import io.github.roony11_1.specification.spring.FilterSpecificationBuilder;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoService
{
    private final ProductoRepository productoRepository;

    public Page<Producto> listar(Map<String, String> filtros, int page, int size, List<String> sort)
    {
        Specification<Producto> base = new FilterSpecificationBuilder<Producto>()
            .withCondition(new FilterCondition("activo", FilterOperator.EQ, true))
            .build();

        Specification<Producto> specification = ProductoFiltrosParser.parse(filtros, base);
        Pageable pageable = ProductoFiltrosParser.pageable(page, size, sort, Sort.by(Sort.Direction.ASC, "nombre"));

        return productoRepository.findAll(specification, pageable);
    }

    public Optional<Producto> obtenerPorId(UUID id)
    {
        return productoRepository.findById(id);
    }

    public List<Producto> obtenerPorIds(List<UUID> ids)
    {
        return productoRepository.findAllById(ids);
    }

    public List<String> listarCategorias()
    {
        return productoRepository.listarCategorias();
    }
}

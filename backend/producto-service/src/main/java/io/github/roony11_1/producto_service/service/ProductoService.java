package io.github.roony11_1.producto_service.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

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

    public List<Producto> listar()
    {
        var specification = new FilterSpecificationBuilder<Producto>()
            .withCondition(new FilterCondition("activo", FilterOperator.EQ, true))
            .build();

        return productoRepository.findAll(specification);
    }

    public Optional<Producto> obtenerPorId(UUID id)
    {
        return productoRepository.findById(id);
    }

    public List<Producto> obtenerPorIds(List<UUID> ids)
    {
        return productoRepository.findAllById(ids);
    }
}

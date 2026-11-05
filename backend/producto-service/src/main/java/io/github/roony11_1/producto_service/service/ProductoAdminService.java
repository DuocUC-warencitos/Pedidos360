package io.github.roony11_1.producto_service.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.roony11_1.producto_service.dto.ProductoAlertaStockResponse;
import io.github.roony11_1.producto_service.dto.ProductoAuditoriaEvent;
import io.github.roony11_1.producto_service.dto.ProductoEvento;
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
    private final KafkaEventPublisher kafkaEventPublisher;

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

    public List<ProductoAlertaStockResponse> alertasStock()
    {
        return productoRepository.listarAlertasStock()
            .stream()
            .map(p -> ProductoAlertaStockResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .sku(p.getSku())
                .stockDisponible(p.getStockDisponible())
                .stockMinimo(p.getStockMinimo())
                .build())
            .toList();
    }

    public Optional<Producto> obtenerPorId(UUID id)
    {
        return productoRepository.findById(id);
    }
    @Transactional 
    public Producto crear(Producto producto) {

        String usuarioId = getUserId();

        Producto guardado = productoRepository.save(producto);

        ProductoAuditoriaEvent evento = new ProductoAuditoriaEvent(
            UUID.randomUUID(),
            guardado.getId(),
            "PRODUCTO_CREADO",
            Instant.now(),
            usuarioId,
            null,
            ProductoEvento.desde(guardado)
        );

        kafkaEventPublisher.publicarAuditoria(evento);

        return guardado;
    }
    @Transactional
    public Optional<Producto> actualizar(UUID id, Producto producto) {

        return productoRepository.findById(id)
            .map(existente -> {

                String usuarioId = getUserId();

                ProductoEvento estadoAnterior =
                    ProductoEvento.desde(existente);

                existente.setNombre(producto.getNombre());
                existente.setPrecio(producto.getPrecio());
                existente.setStockDisponible(producto.getStockDisponible());
                existente.setSku(producto.getSku());
                existente.setCategoria(producto.getCategoria());
                existente.setDescripcion(producto.getDescripcion());
                existente.setShortDescription(producto.getShortDescription());
                existente.setStockMinimo(producto.getStockMinimo());
                existente.setActivo(producto.isActivo());

                Producto actualizado = productoRepository.save(existente);

                ProductoAuditoriaEvent evento = new ProductoAuditoriaEvent(
                    UUID.randomUUID(),
                    actualizado.getId(),
                    "PRODUCTO_ACTUALIZADO",
                    Instant.now(),
                    usuarioId,
                    estadoAnterior,
                    ProductoEvento.desde(actualizado)
                );

                kafkaEventPublisher.publicarAuditoria(evento);

                return actualizado;
            });
    }
    @Transactional
     public boolean eliminar(UUID id) {

        return productoRepository.findById(id)
            .map(producto -> {

                String usuarioId = getUserId();

                ProductoEvento estadoAnterior =
                    ProductoEvento.desde(producto);

                ProductoAuditoriaEvent evento = new ProductoAuditoriaEvent(
                    UUID.randomUUID(),
                    producto.getId(),
                    "PRODUCTO_ELIMINADO",
                    Instant.now(),
                    usuarioId,
                    estadoAnterior,
                    null
                );

                productoRepository.delete(producto);

                kafkaEventPublisher.publicarAuditoria(evento);

                return true;
            })
            .orElse(false);
    }
    private String getUserId() {

        var authentication =
            SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
            || !(authentication.getPrincipal() instanceof Jwt jwt)) {

            throw new AuthenticationCredentialsNotFoundException(
                "No hay un usuario autenticado"
            );
        }

        return jwt.getSubject();
    }
}


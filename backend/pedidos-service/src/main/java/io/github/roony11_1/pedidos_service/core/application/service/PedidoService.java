package io.github.roony11_1.pedidos_service.core.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.roony11_1.error.core.exceptions.InvalidInputException;
import io.github.roony11_1.error.core.exceptions.NotFoundException;
import io.github.roony11_1.pedidos_service.api.dto.response.PedidoStatsResponse;
import io.github.roony11_1.pedidos_service.core.application.event.PedidoEstadoActualizadoEvent;
import io.github.roony11_1.pedidos_service.core.application.interfaces.PedidoEventPublisher;
import io.github.roony11_1.pedidos_service.core.domain.model.EstadoPedido;
import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.pedidos_service.core.domain.model.PedidoProducto;
import io.github.roony11_1.pedidos_service.core.domain.repository.PedidoRepository;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoClient;
import io.github.roony11_1.pedidos_service.infrastructure.client.ProductoClientResiliente;
import io.github.roony11_1.pedidos_service.infrastructure.spec.PedidoFiltrosParser;
import io.github.roony11_1.pedidos_service.infrastructure.spec.SpecificationFactory;
import io.github.roony11_1.pedidos_service.kernel.IUserTokenService;
import io.github.roony11_1.specification.core.FilterCondition;
import io.github.roony11_1.specification.core.FilterOperator;
import io.github.roony11_1.specification.spring.FilterSpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService 
{
    private final PedidoRepository pedidoRepository;
    private final IUserTokenService userTokenService;
    private final ProductoClientResiliente productoClientResiliente;

    @Transactional
    public Pedido save(List<PedidoProducto> productos) 
    {
        var pedido = new Pedido();

        productos.forEach(pedido::addProducto);

        pedido.setUserId(userTokenService.getUserId());
        pedido.setEstadoPedido(EstadoPedido.CREADO);
        pedido.setComentario(userTokenService.getAuditComentario("Ingresado por"));

        var saved = pedidoRepository.save(pedido);
        publicarEstado(saved);

        return saved;
    }

    public Pedido findById(UUID id)
    {
        Specification<Pedido> specification =
            userTokenService.<Pedido>getUserSpecification()
                .and(new FilterSpecificationBuilder<Pedido>()
                        .withCondition(new FilterCondition("id", FilterOperator.EQ, id))
                        .build()
                );

        return pedidoRepository.findOne(specification)
            .orElseThrow(() -> new NotFoundException("pedido", id));
    }

    @Transactional(readOnly = true)
    public Page<Pedido> findAll(Map<String, String> filtros, int page, int size, List<String> sort)
    {
        Specification<Pedido> base = null;

        if (!userTokenService.hasAnyRole("ADMIN", "OPERADOR"))
            base = userTokenService.getUserSpecification();

        Specification<Pedido> specification = PedidoFiltrosParser.parse(filtros, base);
        Pageable pageable = PedidoFiltrosParser.pageable(page, size, sort, Sort.by(Sort.Direction.DESC, "createdAt"));

        return pedidoRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Pedido> findAllByUserId(Map<String, String> filtros, int page, int size, List<String> sort)
    {
        Specification<Pedido> specification = PedidoFiltrosParser.parse(filtros, userTokenService.getUserSpecification());
        Pageable pageable = PedidoFiltrosParser.pageable(page, size, sort, Sort.by(Sort.Direction.DESC, "createdAt"));

        return pedidoRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Pedido> findAllAdmin(Map<String, String> filtros, int page, int size, List<String> sort)
    {
        Specification<Pedido> specification = PedidoFiltrosParser.parse(filtros, null);
        Pageable pageable = PedidoFiltrosParser.pageable(page, size, sort, Sort.by(Sort.Direction.DESC, "createdAt"));

        return pedidoRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Pedido> findAllByUserIdParam(String userId, Map<String, String> filtros, int page, int size, List<String> sort)
    {
        Specification<Pedido> specification = PedidoFiltrosParser.parse(filtros, SpecificationFactory.<Pedido>byUserId(userId));
        Pageable pageable = PedidoFiltrosParser.pageable(page, size, sort, Sort.by(Sort.Direction.DESC, "createdAt"));

        return pedidoRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public PedidoStatsResponse statsAdmin()
    {
        return PedidoStatsResponse.builder()
            .total(pedidoRepository.count())
            .cancelables(pedidoRepository.contarCancelables())
            .cancelados(pedidoRepository.contarCancelados())
            .build();
    }

    /**
     * Administración: un pedido por id sin filtro de usuario.
     * El control de acceso va en el controller (@PreAuthorize ADMIN/OPERADOR).
     */
    @Transactional(readOnly = true)
    public Pedido findByIdAdmin(UUID id)
    {
        return pedidoRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Pedido", id));
    }

    /**
     * Resuelve el pedido a cancelar: ADMIN/OPERADOR pueden cancelar
     * cualquiera; el resto solo los propios (Roony Specification Builder:
     * match x userId AND match x id). Si no es suyo -> 404.
     */
    @Transactional(readOnly = true)
    public Pedido obtenerParaCancelar(UUID id)
    {
        if (userTokenService.hasAnyRole("ADMIN", "OPERADOR"))
            return obtenerPedido(id);

        return obtenerPedidoDelUsuario(id);
    }

    @Transactional
    public void deleteAll()
    {
        var pedidos = pedidoRepository.findAll();

        var estadosConReserva = Set.of(EstadoPedido.STOCK_RESERVADO, EstadoPedido.ACEPTADO, EstadoPedido.CONFIRMADO);
        for (var pedido : pedidos) 
        {
            if (estadosConReserva.contains(pedido.getEstadoPedido()) && !pedido.getProductos().isEmpty()) 
            {
                var liberarReq = new ProductoClient.LiberarStockRequest(
                    pedido.getId(),
                    pedido.getProductos().stream()
                        .map(pp -> new ProductoClient.LiberarStockRequest.Item(pp.getProductoId(), pp.getCantidad()))
                        .toList()
                );

                try 
                {
                    productoClientResiliente.liberar(liberarReq);
                    log.info("Stock liberado deleteAll pedidoId={} estado={} items={}", pedido.getId(), pedido.getEstadoPedido(), liberarReq.items().size());
                } 
                catch (Exception ex) 
                {
                    log.warn("Fallo liberando stock deleteAll pedido {} estado {}: {}", pedido.getId(), pedido.getEstadoPedido(), ex.getMessage());
                }
            }
        }

        pedidoRepository.deleteAll();
    }

    @Transactional
    public void avanzarEstado(UUID id)
    {
        var pedido = obtenerPedido(id);

        EstadoPedido siguiente = pedido.getEstadoPedido().siguiente()
            .orElseThrow(() -> new InvalidInputException("El pedido ya está en un estado final: " + pedido.getEstadoPedido()));

        if ((siguiente == EstadoPedido.EN_PREPARACION || siguiente == EstadoPedido.DESPACHADO) && !pedido.getProductos().isEmpty())
        {
            var confirmarReq = new ProductoClient.ConfirmarStockRequest(
                pedido.getId(),
                pedido.getProductos().stream()
                    .map(pp -> new ProductoClient.ConfirmarStockRequest.Item(pp.getProductoId(), pp.getCantidad()))
                    .toList()
            );

            productoClientResiliente.confirmar(confirmarReq);
            log.info("Stock confirmado pedidoId={} al avanzar a {} items={}", pedido.getId(), siguiente, confirmarReq.items().size());
        }

        String comentario = userTokenService.getAuditComentario("Estado actualizado a " + siguiente + " por");
        pedido.avanzarEstado(comentario);
        publicarEstado(pedido);
    }

    private Pedido obtenerPedido(UUID id) 
    {
        return pedidoRepository.findById(id)
            .orElseThrow(() ->
                new NotFoundException("Pedido", id));
    }

    private Pedido obtenerPedidoDelUsuario(UUID id) 
    {
        Specification<Pedido> specification =
            userTokenService.<Pedido>getUserSpecification()
                .and(SpecificationFactory.<Pedido>byId(id));

        return pedidoRepository.findOne(specification)
            .orElseThrow(() ->
                new NotFoundException("Pedido", id));
    }
    private final PedidoEventPublisher eventPublisher;

    private static final Set<EstadoPedido> ESTADOS_NOTIFICABLES = Set.of(
        EstadoPedido.CREADO,
        EstadoPedido.EN_PREPARACION,
        EstadoPedido.DESPACHADO,
        EstadoPedido.ENTREGADO);

    private void publicarEstado(Pedido pedido)
    {
        if (!ESTADOS_NOTIFICABLES.contains(pedido.getEstadoPedido()))
            return;

        eventPublisher.publicarEstadoActualizado(
            new PedidoEstadoActualizadoEvent(
                pedido.getId(),
                pedido.getUserId(),
                pedido.getEstadoPedido().name(),
                pedido.getCorrelationId(),
                java.time.Instant.now()
            )
        );
    }
}

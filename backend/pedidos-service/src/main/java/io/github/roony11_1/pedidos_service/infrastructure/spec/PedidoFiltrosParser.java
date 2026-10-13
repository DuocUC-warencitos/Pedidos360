package io.github.roony11_1.pedidos_service.infrastructure.spec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import io.github.roony11_1.pedidos_service.core.domain.model.Pedido;
import io.github.roony11_1.specification.core.FilterConditions;
import io.github.roony11_1.specification.core.FilterException;
import io.github.roony11_1.specification.queryparams.QueryParamsFilterParser;
import io.github.roony11_1.specification.spring.FilterSpecificationBuilder;

/**
 * Traduce los query params HTTP a {@link Specification} usando
 * roony-specification-query-params, sin sintaxis propia: los valores viajan
 * en la sintaxis de la librería ({@code ilike:}, {@code in:}, …).
 *
 * <p>Esta capa solo valida el allowlist, aplica aliases, fusiona la spec base
 * y arma el paginado. Contrato público (aliases): {@code estado}
 * (estadoPedido), {@code usuario} (userId). El {@code userId} crudo queda
 * prohibido para no exponer el modelo interno. Campo desconocido u orden
 * inválido produce {@link FilterException} (400 vía
 * roony-specification-error-spring).
 */
public final class PedidoFiltrosParser
{
    private static final Set<String> PERMITIDOS = Set.of(
        "estadoPedido", "estado", "usuario", "comentario", "createdAt", "updatedAt"
    );

    private static final Map<String, String> ALIAS = Map.of(
        "estado", "estadoPedido",
        "usuario", "userId"
    );

    private static final Set<String> ORDENABLE = Set.of(
        "createdAt", "updatedAt", "estadoPedido"
    );

    private static final int TAMANO_MAXIMO = 100;

    private PedidoFiltrosParser()
    {
    }

    public static Specification<Pedido> parse(Map<String, String> params, Specification<Pedido> base)
    {
        Map<String, String> filtros = new LinkedHashMap<>();

        for (var entry : params.entrySet())
        {
            String clave = entry.getKey();

            if (isReservado(clave))
                continue;

            if (!PERMITIDOS.contains(clave))
                throw new FilterException("Filtro no permitido: " + clave);

            String valor = entry.getValue();

            if (valor == null || valor.isBlank())
                continue;

            filtros.put(clave, valor.strip());
        }

        if (filtros.isEmpty())
            return base;

        FilterConditions condiciones = QueryParamsFilterParser.parse(filtros, ALIAS);
        Specification<Pedido> specification = new FilterSpecificationBuilder<Pedido>(condiciones).build();

        if (base == null)
            return specification;

        return base.and(specification);
    }

    public static Pageable pageable(int page, int size, List<String> sort, Sort porDefecto)
    {
        int pagina = Math.max(page, 0);
        int tamano = Math.min(Math.max(size, 1), TAMANO_MAXIMO);

        return PageRequest.of(pagina, tamano, parseSort(sort, porDefecto));
    }

    static Sort parseSort(List<String> sort, Sort porDefecto)
    {
        if (sort == null || sort.isEmpty())
            return porDefecto;

        List<Sort.Order> ordenes = new ArrayList<>();

        for (String criterio : sort)
        {
            if (criterio == null || criterio.isBlank())
                continue;

            String[] partes = criterio.split(",");

            String campo = partes[0].strip();

            if (!ORDENABLE.contains(campo))
                throw new FilterException("Orden no permitido: " + campo);

            Sort.Direction direccion = Sort.Direction.ASC;

            if (partes.length > 1)
            {
                String textoDireccion = partes[1].strip().toLowerCase();

                if (textoDireccion.equals("desc"))
                    direccion = Sort.Direction.DESC;
                else if (!textoDireccion.equals("asc"))
                    throw new FilterException("Dirección de orden inválida: " + partes[1].strip());
            }

            ordenes.add(new Sort.Order(direccion, campo));
        }

        if (ordenes.isEmpty())
            return porDefecto;

        return Sort.by(ordenes);
    }

    private static boolean isReservado(String clave)
    {
        return clave.equals("page") || clave.equals("size") || clave.equals("sort");
    }
}

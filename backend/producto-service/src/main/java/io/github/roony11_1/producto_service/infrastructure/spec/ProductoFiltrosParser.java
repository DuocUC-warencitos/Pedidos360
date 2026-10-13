package io.github.roony11_1.producto_service.infrastructure.spec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

import io.github.roony11_1.producto_service.model.Producto;
import io.github.roony11_1.specification.core.FilterConditions;
import io.github.roony11_1.specification.core.FilterException;
import io.github.roony11_1.specification.queryparams.QueryParamsFilterParser;
import io.github.roony11_1.specification.spring.FilterSpecificationBuilder;

/**
 * Traduce los query params HTTP a {@link Specification} usando
 * roony-specification-query-params, sin sintaxis propia: los valores viajan
 * en la sintaxis de la librería ({@code ilike:}, {@code gte:}, {@code in:}…).
 *
 * <p>Esta capa solo valida el allowlist, aplica aliases, fusiona la spec base
 * y arma el paginado. Contrato público (aliases): {@code q} (nombre),
 * {@code cat} (categoria), {@code desde}/{@code hasta} (precio).
 * Campo desconocido u orden inválido produce {@link FilterException}
 * (400 vía roony-specification-error-spring).
 */
public final class ProductoFiltrosParser
{
    private static final Set<String> PERMITIDOS = Set.of(
        "nombre", "sku", "categoria", "shortDescription", "descripcion",
        "precio", "stockDisponible", "stockMinimo", "activo", "createdAt",
        "q", "cat", "desde", "hasta"
    );

    private static final Map<String, String> ALIAS = Map.of(
        "q", "nombre",
        "cat", "categoria"
    );

    private static final Set<String> ORDENABLE = Set.of(
        "nombre", "precio", "createdAt", "stockDisponible", "categoria"
    );

    private static final int TAMANO_MAXIMO = 100;

    private ProductoFiltrosParser()
    {
    }

    public static Specification<Producto> parse(Map<String, String> params, Specification<Producto> base)
    {
        Map<String, String> filtros = new LinkedHashMap<>();

        Integer desde = null;
        Integer hasta = null;

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

            // Rango de precio con spec manual tipada: la librería no soporta
            // GT/GTE/LT/LTE/BETWEEN (JpaPredicateBuilder hace path.as(Comparable),
            // que Hibernate rechaza). Se acepta con o sin prefijo gte:/lte:.
            if (clave.equals("desde"))
            {
                desde = parseEntero(clave, valor);
                continue;
            }

            if (clave.equals("hasta"))
            {
                hasta = parseEntero(clave, valor);
                continue;
            }

            filtros.put(clave, valor.strip());
        }

        Specification<Producto> specification = base;

        if (!filtros.isEmpty())
        {
            FilterConditions condiciones = QueryParamsFilterParser.parse(filtros, ALIAS);
            specification = and(specification, new FilterSpecificationBuilder<Producto>(condiciones).build());
        }

        specification = and(specification, rangoPrecio(desde, hasta));

        return specification;
    }

    private static Specification<Producto> rangoPrecio(Integer desde, Integer hasta)
    {
        if (desde == null && hasta == null)
            return null;

        return (root, query, cb) ->
        {
            List<Predicate> predicados = new ArrayList<>();

            if (desde != null)
                predicados.add(cb.ge(root.get("precio"), desde));

            if (hasta != null)
                predicados.add(cb.le(root.get("precio"), hasta));

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private static Specification<Producto> and(Specification<Producto> a, Specification<Producto> b)
    {
        if (a == null)
            return b;

        if (b == null)
            return a;

        return a.and(b);
    }

    private static Integer parseEntero(String clave, String valor)
    {
        String limpio = valor.strip().replaceFirst("(?i)^(gte|lte|gt|lt)\\s*[:|]\\s*", "");

        try
        {
            return Integer.valueOf(limpio);
        }
        catch (NumberFormatException ex)
        {
            throw new FilterException("Filtro inválido: " + clave + " debe ser un número entero", ex);
        }
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

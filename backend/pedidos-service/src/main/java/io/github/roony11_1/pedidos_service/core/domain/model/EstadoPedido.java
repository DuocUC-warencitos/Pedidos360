package io.github.roony11_1.pedidos_service.core.domain.model;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public enum EstadoPedido 
{
    CREADO,
    CONFIRMADO,
    EN_PREPARACION,
    DESPACHADO,
    ENTREGADO,
    CANCELADO;

    private static final Map<EstadoPedido, EstadoPedido> FLUJO = Map.of(
        CREADO,         CONFIRMADO,
        CONFIRMADO,     EN_PREPARACION,
        EN_PREPARACION, DESPACHADO,
        DESPACHADO,     ENTREGADO
        // ENTREGADO y CANCELADO no están: no tienen siguiente
    );

        private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
        CREADO,         Set.of(CONFIRMADO, CANCELADO),
        CONFIRMADO,     Set.of(EN_PREPARACION, CANCELADO),
        EN_PREPARACION, Set.of(DESPACHADO, CANCELADO),
        DESPACHADO,     Set.of(ENTREGADO),
        ENTREGADO,      Set.of(),
        CANCELADO,      Set.of()
    );

    /** Devuelve el siguiente estado del flujo principal, o vacío si es final. */
    public Optional<EstadoPedido> siguiente()
    {
        return Optional.ofNullable(FLUJO.get(this));
    }

    /** ¿Es válida esta transición (incluye cancelar)? */
    public boolean puedeTransicionarA(EstadoPedido nuevo)
    {
        return TRANSICIONES.getOrDefault(this, Set.of()).contains(nuevo);
    }

    public boolean esFinal()
    {
        return this == ENTREGADO || this == CANCELADO;
    }
}

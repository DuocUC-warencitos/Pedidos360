package io.github.roony11_1.pedidos_service.core.domain.model;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public enum EstadoPedido 
{
    CREADO,
    STOCK_RESERVADO,
    STOCK_FALLIDO,
    ACEPTADO,
    /**
     * @deprecated Mantener por compatibilidad BD. Usar ACEPTADO.
     */
    @Deprecated
    CONFIRMADO,
    EN_PREPARACION,
    DESPACHADO,
    ENTREGADO,
    CANCELADO;

    private static final Map<EstadoPedido, EstadoPedido> FLUJO = Map.of(
        CREADO,          STOCK_RESERVADO,
        STOCK_RESERVADO, ACEPTADO,
        ACEPTADO,        EN_PREPARACION,
        EN_PREPARACION,  DESPACHADO,
        DESPACHADO,      ENTREGADO
    );

    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
        CREADO,          Set.of(STOCK_RESERVADO, STOCK_FALLIDO, CANCELADO),
        STOCK_RESERVADO, Set.of(ACEPTADO, CONFIRMADO, CANCELADO),
        STOCK_FALLIDO,   Set.of(CANCELADO),
        ACEPTADO,        Set.of(EN_PREPARACION, CANCELADO),
        CONFIRMADO,      Set.of(EN_PREPARACION, CANCELADO),
        EN_PREPARACION,  Set.of(DESPACHADO, CANCELADO),
        DESPACHADO,      Set.of(ENTREGADO, CANCELADO),
        ENTREGADO,       Set.of(),
        CANCELADO,       Set.of()
    );

    public Optional<EstadoPedido> siguiente()
    {
        return Optional.ofNullable(FLUJO.get(this));
    }

    public boolean puedeTransicionarA(EstadoPedido nuevo)
    {
        return TRANSICIONES.getOrDefault(this, Set.of()).contains(nuevo);
    }

    public boolean esFinal()
    {
        return this == ENTREGADO || this == CANCELADO || this == STOCK_FALLIDO;
    }
}

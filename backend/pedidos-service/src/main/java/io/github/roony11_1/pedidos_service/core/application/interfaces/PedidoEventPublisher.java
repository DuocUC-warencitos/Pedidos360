package io.github.roony11_1.pedidos_service.core.application.interfaces;

import io.github.roony11_1.pedidos_service.core.application.event.PedidoCreadoEvent;
import io.github.roony11_1.pedidos_service.core.application.event.PedidoEstadoCanceladoEvent;
import io.github.roony11_1.pedidos_service.core.application.event.PedidoStockFallidoEvent;
import io.github.roony11_1.pedidos_service.core.application.event.PedidoStockReservadoEvent;
import io.github.roony11_1.pedidos_service.core.application.event.PedidoEstadoActualizadoEvent;
public interface PedidoEventPublisher
{
    void publicarCreado(PedidoCreadoEvent event);

    void publicarCreadoFinal();

    void publicarStockReservado(PedidoStockReservadoEvent event);

    void publicarStockFallido(PedidoStockFallidoEvent event);

    void publicarEstadoCancelado(PedidoEstadoCanceladoEvent event);

    void publicarEstadoActualizado(PedidoEstadoActualizadoEvent event);
}

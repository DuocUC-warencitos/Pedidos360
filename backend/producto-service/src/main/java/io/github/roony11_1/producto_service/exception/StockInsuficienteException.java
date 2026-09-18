package io.github.roony11_1.producto_service.exception;

import java.util.UUID;

public class StockInsuficienteException extends RuntimeException
{
    public StockInsuficienteException(UUID productoId, int solicitado, int disponible)
    {
        super("Stock insuficiente para producto: %s, solicidato: %d, disponible: %d".formatted(productoId, solicitado, disponible));
    }
}

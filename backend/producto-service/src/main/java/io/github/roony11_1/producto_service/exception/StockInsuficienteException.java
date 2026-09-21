package io.github.roony11_1.producto_service.exception;

import java.util.UUID;

import io.github.roony11_1.error.core.StandardErrorCategories;
import io.github.roony11_1.error.core.exceptions.AppException;

public class StockInsuficienteException extends AppException
{
    public StockInsuficienteException(UUID productoId, int solicitado, int disponible)
    {
        super("ERR-1001",
              "Stock insuficiente para producto: %s, solicitado: %d, disponible: %d".formatted(productoId, solicitado, disponible),
              StandardErrorCategories.ALREADY_EXISTS,
              "Stock insuficiente para producto: %s, solicitado: %d, disponible: %d".formatted(productoId, solicitado, disponible));
    }
}

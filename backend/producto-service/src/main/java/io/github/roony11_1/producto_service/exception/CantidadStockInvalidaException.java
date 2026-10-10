package io.github.roony11_1.producto_service.exception;

import io.github.roony11_1.error.core.StandardErrorCategories;
import io.github.roony11_1.error.core.exceptions.AppException;

public class CantidadStockInvalidaException extends AppException
{
    public CantidadStockInvalidaException(int cantidad)
    {
        super(
            "ERR-1003",
            "La cantidad de stock debe ser mayor que cero: %d".formatted(cantidad),
            StandardErrorCategories.ALREADY_EXISTS,
            "La cantidad de stock debe ser mayor que cero: %d".formatted(cantidad)
        );
    }
}
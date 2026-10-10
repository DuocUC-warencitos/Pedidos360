package io.github.roony11_1.producto_service.exception;

import java.util.UUID;

import io.github.roony11_1.error.core.StandardErrorCategories;
import io.github.roony11_1.error.core.exceptions.AppException;

public class StockReservadoInsuficienteException extends AppException
{
    public StockReservadoInsuficienteException(
        UUID productoId,
        int solicitado,
        int reservado
    )
    {
        super(
            "ERR-1002",
            "Stock reservado insuficiente para producto: %s, solicitado: %d, reservado: %d"
                .formatted(productoId, solicitado, reservado),
            StandardErrorCategories.ALREADY_EXISTS,
            "Stock reservado insuficiente para producto: %s, solicitado: %d, reservado: %d"
                .formatted(productoId, solicitado, reservado)
        );
    }
}
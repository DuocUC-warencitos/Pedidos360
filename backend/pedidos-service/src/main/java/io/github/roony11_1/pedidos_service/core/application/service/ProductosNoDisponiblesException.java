package io.github.roony11_1.pedidos_service.core.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.roony11_1.error.core.StandardErrorCategories;
import io.github.roony11_1.error.core.exceptions.AppException;

/**
 * El carrito trae productos inexistentes/inactivos o sin stock suficiente.
 * Se lanza ANTES de persistir: no se guarda pedido ni job, y el frontend
 * usa {@link #detalles()} para quitarlos del carrito.
 */
public class ProductosNoDisponiblesException extends AppException
{
    private final List<UUID> faltantes;
    private final List<DisponibilidadPedidoValidator.ItemSinStock> sinStock;

    public ProductosNoDisponiblesException(
        List<UUID> faltantes,
        List<DisponibilidadPedidoValidator.ItemSinStock> sinStock)
    {
        super("ERR-1002", mensaje(faltantes, sinStock),
            StandardErrorCategories.ALREADY_EXISTS, mensaje(faltantes, sinStock));
        this.faltantes = List.copyOf(faltantes);
        this.sinStock = List.copyOf(sinStock);
    }

    public List<String> detalles()
    {
        var d = new ArrayList<String>();
        faltantes.forEach(id -> d.add("NO_EXISTE:" + id));
        sinStock.forEach(i -> d.add("SIN_STOCK:" + i.productoId()
            + ":solicitado=" + i.solicitado() + ":disponible=" + i.disponible()));
        return d;
    }

    private static String mensaje(
        List<UUID> faltantes,
        List<DisponibilidadPedidoValidator.ItemSinStock> sinStock)
    {
        return "Productos no disponibles: %d faltantes, %d sin stock suficiente"
            .formatted(faltantes.size(), sinStock.size());
    }
}

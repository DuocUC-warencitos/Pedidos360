package io.github.roony11_1.producto_service.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDetailResponse
{
    private UUID id;
    private String nombre;
    private int precio;
    private int stockDisponible;
    private int stockReservado;
    private String sku;
    private String categoria;
    private String descripcion;
    private int stockMinimo;
    private boolean activo;
    private Instant createdAt;
    private Instant updatedAt;
}
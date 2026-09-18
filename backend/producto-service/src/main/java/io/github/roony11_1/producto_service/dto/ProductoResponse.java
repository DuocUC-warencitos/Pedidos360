package io.github.roony11_1.producto_service.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponse 
{
    private UUID id;
    private String nombre;
    private int precio;
    private int stockDisponible;
}
package io.github.roony11_1.producto_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRequest
{
    @NotBlank
    @Size(max = 255)
    private String nombre;

    @Min(1)
    private int precio;

    @Min(0)
    private int stock;

    @Size(max = 50)
    private String sku;

    @Size(max = 100)
    private String categoria;

    @Size(max = 1000)
    private String descripcion;

    @Min(0)
    @Builder.Default
    private int stockMinimo = 0;
}
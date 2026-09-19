package io.github.roony11_1.producto_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
    private String nombre;
    @Min(1)
    private int precio;
    @Min(0)
    private int stock;
}
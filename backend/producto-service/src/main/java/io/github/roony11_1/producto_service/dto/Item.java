package io.github.roony11_1.producto_service.dto;

import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
public class Item 
{
    private UUID productoId;
    private int cantidad;
}

package io.github.roony11_1.producto_service.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor 
@AllArgsConstructor 
@Data 
public class ProductoSnapshotResponse 
{
    private UUID id;
    private String nombre;
    private int precio;
}

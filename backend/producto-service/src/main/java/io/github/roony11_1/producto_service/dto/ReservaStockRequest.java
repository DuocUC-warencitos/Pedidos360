package io.github.roony11_1.producto_service.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReservaStockRequest 
{
    @NotNull
    private UUID pedidoId;
    @NotEmpty
    private List<Item> productos;
}

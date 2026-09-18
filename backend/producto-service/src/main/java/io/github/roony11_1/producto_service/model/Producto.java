package io.github.roony11_1.producto_service.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "producto")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Producto
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "precio")
    private int precio;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "stock_disponible", nullable = false)
    private int stockDisponible;

    @Column(name = "stock_reservado", nullable = false)
    private int stockReservado;

    @Version
    private Long version;

    public void reservarStock(int cantidad)
    {
        stockDisponible = stockDisponible - cantidad;
        stockReservado = stockReservado + cantidad;
    }
}

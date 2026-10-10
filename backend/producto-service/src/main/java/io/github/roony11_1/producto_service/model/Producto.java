package io.github.roony11_1.producto_service.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import io.github.roony11_1.producto_service.exception.CantidadStockInvalidaException;
import io.github.roony11_1.producto_service.exception.StockInsuficienteException;
import io.github.roony11_1.producto_service.exception.StockReservadoInsuficienteException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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

    @Column(nullable = false)
    private int precio;

    @Column(nullable = false)
    private String nombre;

    @Column(unique = true, length = 50)
    private String sku;

    @Column(length = 100)
    private String categoria;

    @Column(length = 1000)
    private String descripcion;

    @Column(name = "stock_disponible", nullable = false)
    private int stockDisponible;

    @Column(name = "stock_reservado", nullable = false)
    private int stockReservado;

    @Column(name = "stock_minimo", nullable = true)
    private Integer stockMinimo;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @CreationTimestamp 
    private Instant createdAt;

    @UpdateTimestamp 
    private Instant updatedAt;

    @Version
    private Long version;

    public void reservarStock(int cantidad)
    {
        validarCantidad(cantidad);

        if (cantidad > stockDisponible)
            throw new StockInsuficienteException(id, cantidad, stockDisponible);

        stockDisponible -= cantidad;
        stockReservado += cantidad;
    }

    public void liberarStock(int cantidad)
    {
        validarCantidad(cantidad);

        if (cantidad > stockReservado)
            throw new StockReservadoInsuficienteException(id, cantidad, stockReservado);

        stockDisponible += cantidad;
        stockReservado -= cantidad;
    }

    private void validarCantidad(int cantidad)
    {
        if (cantidad <= 0)
            throw new CantidadStockInvalidaException(cantidad);
    }
}
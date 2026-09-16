package io.github.roony11_1.pedidos_service.core.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pedido")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pedido 
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column
    private String userId;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<PedidoProducto> productos = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private EstadoPedido estadoPedido = EstadoPedido.CREADO;

    @Column(length = 1000)
    private String comentario;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public void addProducto(PedidoProducto producto)
    {
        productos.add(producto);
        producto.setPedido(this);
    }

    public void cambiarEstado(EstadoPedido nuevo)
    {
        if (!this.estadoPedido.puedeTransicionarA(nuevo))
            throw new IllegalArgumentException(this.estadoPedido + " no puede transicionar a: " + nuevo);

        this.estadoPedido = nuevo;
    }

    private void appendComentario(String nuevo) {
        if (nuevo == null || nuevo.isBlank()) return;
        if (this.comentario == null || this.comentario.isBlank()) {
            this.comentario = nuevo;
        } else {
            this.comentario = this.comentario + "\n" + nuevo;
        }
    }

    /** Avanza al siguiente estado del flujo principal. */
    public void avanzarEstado(String comentario)
    {
        EstadoPedido siguiente = this.estadoPedido.siguiente()
            .orElseThrow(() -> new IllegalStateException("El pedido ya está en un estado final: " + this.estadoPedido));

        cambiarEstado(siguiente);
        appendComentario(comentario);
    }

    public void cancelar(String comentario)
    {
        cambiarEstado(EstadoPedido.CANCELADO);
        appendComentario(comentario);
    }

    // Sobrecargas sin comentario para compatibilidad interna / tests
    public void avanzarEstado() {
        avanzarEstado(null);
    }

    public void cancelar() {
        cancelar(null);
    }
}

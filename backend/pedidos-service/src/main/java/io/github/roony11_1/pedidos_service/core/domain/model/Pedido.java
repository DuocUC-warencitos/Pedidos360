package io.github.roony11_1.pedidos_service.core.domain.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity()
@Table(name = "pedido")
@Data
@AllArgsConstructor
@NoArgsConstructor 
public class Pedido 
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "pedido_producto")
    private List<String> productos = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private EstadoPedido estadoPedido = EstadoPedido.CREADO;
}

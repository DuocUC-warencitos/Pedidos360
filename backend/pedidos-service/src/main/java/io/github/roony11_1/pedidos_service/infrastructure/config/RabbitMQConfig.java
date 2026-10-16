package io.github.roony11_1.pedidos_service.infrastructure.config;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig
{
    public static final String TOPIC = "pedidos360.topic";
    public static final String DLX = "pedidos360.dlx";

    public static final String PEDIDOS = "pedidos360.pedidos.queue";
    public static final String PEDIDOS_DLQ = "pedidos360.pedidos.queue.dlq";

    public static final String RK_PEDIDO_CREAR = "pedido.crear";
    public static final String RK_PEDIDO_CREADO = "pedido.creado";
    public static final String RK_STOCK_RESERVADO = "pedido.stock.reservado";
    public static final String RK_STOCK_FALLIDO = "pedido.stock.fallido";
    public static final String RK_ESTADO_CANCELADO = "pedido.estado.cancelado";

    public static final String DLQ_ROUTING_KEY = "pedidos.failed";

    @Bean
    TopicExchange pedidosTopicExchange()
    {
        return new TopicExchange(TOPIC, true, false);
    }

    @Bean
    DirectExchange pedidosDlx()
    {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    Queue pedidosQueue()
    {
        return QueueBuilder.durable(PEDIDOS)
            .withArguments(Map.of(
                "x-dead-letter-exchange", DLX,
                "x-dead-letter-routing-key", DLQ_ROUTING_KEY))
            .build();
    }

    @Bean
    Queue pedidosDlq()
    {
        return new Queue(PEDIDOS_DLQ, true);
    }

    @Bean
    Binding pedidosBinding()
    {
        return BindingBuilder.bind(pedidosQueue()).to(pedidosTopicExchange()).with(RK_PEDIDO_CREAR);
    }

    @Bean
    Binding pedidosDlqBinding()
    {
        return BindingBuilder.bind(pedidosDlq()).to(pedidosDlx()).with(DLQ_ROUTING_KEY);
    }
}
